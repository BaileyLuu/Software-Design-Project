package ca.yorku.eecs;

import java.io.IOException;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;
//import static org.neo4j.driver.Values.parameters;
//import org.neo4j.driver.AuthTokens;
//import org.neo4j.driver.Driver;
//import org.neo4j.driver.GraphDatabase;
//import org.neo4j.driver.Result;
//import org.neo4j.driver.Session;
//import org.neo4j.driver.Transaction;
import static org.neo4j.driver.v1.Values.parameters;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.Transaction;

public class App 
{
    static int PORT = 8080; //remember to change to port 8080 before submission
    
    
    public static void main(String[] args) throws IOException
    {
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);
        
        Handler film = new Handler();
        
        server.createContext("/", film::handle);
        
        server.start();
        
        System.out.printf("Server started on port %d...\n", PORT);
    }
}
