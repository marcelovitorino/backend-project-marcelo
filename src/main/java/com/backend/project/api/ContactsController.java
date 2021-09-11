package com.backend.project.api;

import com.backend.project.entities.Contact;
import com.google.gson.Gson;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/contacts")
public class ContactsController {

    public static final String ACESS_TOKEN_SENDGRID = "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";
    public static final String ACESS_TOKEN_MAILCHIMP = "92f72c84333808236eb2324d216d4091-us5";
    public static final String URI_MAILCHIMP = "https://us5.api.mailchimp.com/3.0/";
    public static final String URI_SENDGRID = "https://api.sendgrid.com/v3/marketing/contacts";

    /**
     * Endpoint responsible for synchronizing mailchimp contacts to sendgrid.
     *
     * @return number of synchronized contacts and the contacts
     */
    @RequestMapping(value = "/sync", method = RequestMethod.GET)
    public ResponseEntity synchronizeContacts() {
        try {
            List<Contact> diffContacts = this.syncInSendGrid(this.getContacts());
            Map<String, Object> response = new HashMap<>();
            response.put("syncedContacts", diffContacts.size());
            response.put("contacts", diffContacts);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Method responsible for getting the available contacts. for this, the available
     * lists are consulted and subsequently their members.
     *
     * @return list of available contacts
     */
    private List<Contact> getContacts() {
        final String uriLists = URI_MAILCHIMP + "lists?fields=lists.id&count=1000";
        ResponseEntity<Map> resultLists = this.makeGetRequest(uriLists, ACESS_TOKEN_MAILCHIMP);
        List<Map<String, String>> lists = (List<Map<String, String>>) resultLists.getBody().get("lists");

        List<Contact> contacts = new ArrayList<>();
        for (Map<String, String> list : lists) {
            contacts.addAll(this.getMembers(list.get("id")));
        }
        return contacts;
    }

    /**
     * Method responsible for getting the available members for each list identifier
     * provided, turning into contacts.
     *
     * @param id identifier of list in mailchip
     * @return list of available contacts
     */
    private List<Contact> getMembers(String id) {
        final String uriMembers = URI_MAILCHIMP + "lists/" + id + "/members?count=1000&fields=members.full_name,members.email_address";
        ResponseEntity<Map> resultMembers = this.makeGetRequest(uriMembers, ACESS_TOKEN_MAILCHIMP);
        List<Map<String, String>> members = (List<Map<String, String>>) resultMembers.getBody().get("members");

        List<Contact> contacts = new ArrayList<>();
        for (Map<String, String> member : members) {
            Contact contact = new Contact();
            String[] fullName = member.get("full_name").split(" ");
            if (fullName != null && !fullName[0].isBlank()) {
                contact.setFirstName(fullName[0]);
                contact.setLastName(fullName.length > 1 ? fullName[1] : null);
            }
            contact.setEmail(member.get("email_address"));
            contacts.add(contact);
        }
        return contacts;
    }

    /**
     * Reusable method responsible for performing http get requests, using the access token provided.
     *
     * @param uri        adress of the resource
     * @param acessToken access token provided
     * @return the answer found by the request
     */
    private ResponseEntity<Map> makeGetRequest(String uri, String acessToken) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(acessToken);
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
        return restTemplate.exchange(uri, HttpMethod.GET, entity, Map.class);
    }

    /**
     * Reusable method responsible for performing http post requests, using the access token provided and the body.
     *
     * @param uri        adress of the resource
     * @param acessToken access token provided
     * @param body       body of the request
     * @return the answer found by the request
     */
    private ResponseEntity<Map> makePostRequest(String uri, String acessToken, String body) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(acessToken);
        return restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(body, headers), Map.class, body);
    }

    /**
     * Method responsible for synchronizing contacts. For this, the existing contacts in sendgrid
     * are taken and their difference between the available contacts is included and returned.
     *
     * @param contacts list of available contacts
     * @return list of included contacts
     * @throws IOException
     */
    private List<Contact> syncInSendGrid(List<Contact> contacts) throws IOException {
        ResponseEntity<Map> resultContacts = this.makeGetRequest(URI_SENDGRID, ACESS_TOKEN_SENDGRID);
        List<Map<String, String>> actualContacts = (List<Map<String, String>>) resultContacts.getBody().get("result");
        Predicate<Contact> notExistEmail = contact -> !actualContacts.stream().anyMatch(c -> c.get("email").equals(contact.getEmail()));
        List<Contact> diffContacts = contacts.stream().filter(notExistEmail).collect(Collectors.toList());

        if (diffContacts.size() > 0) {
            ResponseEntity<Map> result = this.makePostRequest(URI_SENDGRID, ACESS_TOKEN_SENDGRID, new Gson().toJson(diffContacts));
            System.out.println(result.getStatusCode());
            System.out.println(result.getBody());
            if (!result.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("It wasn't possible synced contacts! Error in sendgrid request.");
            }
        }
        return diffContacts;
    }

}
