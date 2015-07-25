package controllers;
import models.*;
import play.mvc.*;

import views.html.*;

public class Application extends Controller {

    public static Client currentClient;
    public static String theOtherUserName;

    // index to landing page by default
    public static Result index() {
        return redirect(routes.Application.landing());
    }

    public static Result landing() {
        currentClient = null;
        return ok(landing.render());
    }

    // get the information that the client just typed
    public static Client getClient() {
        play.data.Form<Client> clientForm = play.data.Form.form(Client.class);
        return clientForm.bindFromRequest().get();
    }

    // methods deal with client account
    public static  Result createClient() {

        Client client = getClient();

        if(Client.checkNull(client) || Client.checkEmpty(client)){
            return redirect(routes.Application.register("Empty details"));
        } else if(!client.emailFormat()){
            return redirect(routes.Application.register("Wrong email format"));
        } else if(!Client.checkPasswordConsistency(client)){
            return redirect(routes.Application.register("Passwords do not match"));
        } else if(Client.clientEmailExists(client)){
            return redirect(routes.Application.register("Client with email " + client.email + " already exists"));
        }
        Client.createClient(client);
        currentClient = client;
        return ok(randChat.render(currentClient, RandChat.getWaiting(), RandChat.getChatPairs(), Unread.getUnreadNum(currentClient)));
    }

    public static  Result deleteClientFromDB(String email) {
        Client.deleteClient(email);
        return redirect(routes.Application.displayAllClientsFromDB());
    }

    public static  Result displayAllClientsFromDB() {
        return ok(registration.render(Client.find.findList(), ""));
    }

    public static Result logIn() {
        Client client = getClient();
        if(Client.validate(client)) {
            currentClient = Client.findClient(client);
            return ok(randChat.render(currentClient, RandChat.getWaiting(), RandChat.getChatPairs(), Unread.getUnreadNum(currentClient)));
        }
        return ok(landing.render());
    }

    public  static Result register(String error) {
        return ok(registration.render(Client.find.findList(), error));
    }



    // Websocket interface
    public static WebSocket<String> wsInterface(){
        return new WebSocket<String>(){
            // called when websocket handshake is done
            public void onReady(WebSocket.In<String> in, WebSocket.Out<String> out){
                RandChat.start(in, out, currentClient);
            }
        };
    }

    public static Result nextUser(){

     //RandChat.nextUser(currentClient);

      return ok("hello");

    }


    public static Result wsJs() {
        return ok(views.js.ws.render());
    }

    public static Result findFriendProfileWithClientEmail(String clientEmail) {
        Client client = Client.findClientByEmail(clientEmail);

        return ok(friendProfile.render(client, Unread.friendRequestReceived(currentClient, client)));

    }

    public static Result friendProfile() {
        ClientConnection clientConnection = RandChat.findClientConnection(currentClient);
        if(clientConnection != null && clientConnection.isPaired()) {
            Client client2 = clientConnection.getChatPair().getTheOtherClientConnection(clientConnection).getClient();
            return ok(friendProfile.render(client2, Unread.friendRequestReceived(currentClient, client2)));
        }
        return ok("Not connected to a user yet, please wait for the next user. :)");
    }

    public static Result addFriend(String friendRequestClientEmail) {
        Unread.createUnreadFriendRequest(currentClient, friendRequestClientEmail);
        return ok("Friend request sent, waiting to be comfirmed..");
    }

    public static Result showUnread() {
        if(Unread.getUnreadNum(currentClient) == 0) {
            return ok("Everything has been read.");
        } else {
            return ok(unread.render(Unread.getUnreadFriendRequest(currentClient)));
        }
    }


}
