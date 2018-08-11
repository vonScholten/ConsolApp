/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rmi.implementions;

import rmi.interfaces.HangmanClientInterface;
import rmi.interfaces.ClientInterface;
import brugerautorisation.data.Bruger;
import brugerautorisation.data.Diverse;
import brugerautorisation.transport.rmi.Brugeradmin;
import hangman.Gamelogic;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import soap.RemoteI;

/**
 *
 * @author s145005
 */
public class HangmanClientImplementation extends UnicastRemoteObject implements HangmanClientInterface {

    private final List<ClientInterface> clients = new ArrayList<>();
    private Brugeradmin ba;
    private Bruger b;
    private URL url;
    private QName qname;
    private Service service;
    private RemoteI r;
    
    public HangmanClientImplementation() throws RemoteException, MalformedURLException {
        super();
        
        url = new URL("http://localhost:9092/soap?wsdl");
        qname = new QName("http://soap/", "RemoteImplService");
        service = Service.create(url, qname);
        r = service.getPort(RemoteI.class);
        System.out.println(r.handshake());
    }

    @Override
    public String handshake() throws RemoteException {

        return "handshake";
    }

    @Override
    public void register(String username, String password, ClientInterface client) throws RemoteException {
        String loginData = null;

        try {
            ba = (Brugeradmin) Naming.lookup("rmi://javabog.dk/brugeradmin");
            b = ba.hentBruger(username, password);

            loginData = "User: " + b + ", " + "Data: " + Diverse.toString(b);

        } catch (NotBoundException | MalformedURLException ex) {
            Logger.getLogger(HangmanClientImplementation.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (!(clients.contains(client))) {
            clients.add(client);
//            Object score = ba.getEkstraFelt(username, password, "score");
//            String scoreString = score.toString();
//            Highscore user = new Highscore(username, scoreString);
//            highscore.add(user);
            System.out.println("registered client " + client);
        }

        doCallback(client);

    }

    @Override
    public void unregister(ClientInterface client) throws RemoteException {
        if (clients.remove(client)) {
            System.out.println("unregistered client " + client);
        } else {
            System.err.println("client does not exist");
        }
    }

    public void doCallback(ClientInterface client) throws RemoteException {

        Gamelogic game = new Gamelogic();
       
        client.callback("\nNEW GAME");
        game.reset();
        System.out.println("word is: " + game.getWord());
        
        int round = 0;
        
        while (!game.isGameover()) {
            round = round + 1;
            client.callback("\n--------- round " + round + " ---------");
            client.callback("Your nummer of wrongs is: " + game.getWrongs());
            client.callback("The current visible word is: " + game.getVisible());
            client.callback("Used letters: " + game.getUsedLetters());
            
            String letter = client.input("Guess a letter: ");
            game.check(letter);
            
            if(game.isCorrect()){
                client.callback(letter + " Was correct");
                client.callback("--------- round " + round + " ---------");
            }
            else {
                client.callback(letter + " Ss not in the word try again");
                client.callback("--------- round " + round + " ---------");
            }

            if (game.isGameover()){
                if(game.isWon()){
                    client.callback("GRATZ YOU WON");
                }
                else{
                    client.callback("YOU LOST BETTER LUCK NEXT TIME");
                }
            }
        }
        
        String choice = client.input("\nDo you want to play a new game (y/n)");
        if("y".equals(choice)){
            doCallback(client);
        }
        else {
            client.callback("\nGoodbye");
        }
    }
}
