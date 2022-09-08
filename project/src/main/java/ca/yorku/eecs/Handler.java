package ca.yorku.eecs;


import java.net.*;
import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import org.json.*;
import org.neo4j.driver.v1.*;


import com.sun.net.httpserver.*;


public class Handler  implements HttpHandler {

	@Override
	public void handle(HttpExchange request) throws IOException {
		// TODO Auto-generated method stub

		try {
			if (request.getRequestMethod().equals("GET")) {
				handleGet(request);
				System.out.println("get");
			} else if(request.getRequestMethod().equals("PUT")) {
				handlePut(request);
			}else {
				sendString(request, "Unimplemented method\n", 501);
			}

		} catch (Exception e) {
			e.printStackTrace();
			sendString(request, "Server error\n", 500);
		}

	}

	private void sendString(HttpExchange request, String data, int restCode) 
			throws IOException {
		request.sendResponseHeaders(restCode, data.length());
		OutputStream os = request.getResponseBody();
		os.write(data.getBytes());
		os.close();
	}

	private void handlePut(HttpExchange request) throws IOException {

		InputStream is = request.getRequestBody();
		String requestBody = Utils.convert(is);
		System.out.println(requestBody);


		try {

			JSONObject obj = new JSONObject(requestBody);
			Neo4jFilm nb = new Neo4jFilm();
			String name;
			String actorId;
			String movieId;
			String eventId;
			String categoryId;
			String response;
//			boolean check;
			int year;
			if(obj.length() == 2 && obj.has("name") && obj.has("actorId")) {	//Add Actor node
				name = obj.getString("name");
				actorId = obj.getString("actorId");
				System.out.println("name: " + name + "\tactorId: " + actorId);
				nb.insertActor(name, actorId);
				nb.close();
				System.out.println(obj.toString());
				
				sendString(request, obj.toString(), 200);
				
			}else if(obj.length() == 2 && obj.has("name") && obj.has("movieId")){	//Add Movie node
				name = obj.getString("name");
				movieId = obj.getString("movieId");
				nb.insertMovie(name, movieId);
				nb.close();
				sendString(request, obj.toString(), 200);
			}else if(obj.length() == 2 && obj.has("actorId") && obj.has("movieId")){ //Add ACTED_IN relationship between actor and movie
				actorId = obj.getString("actorId");
				movieId = obj.getString("movieId");
				nb.insertActedIn(actorId, movieId);
				
				nb.close();
				sendString(request, obj.toString(), 200);
			}else if(obj.length() == 2 && obj.has("eventName") && obj.has("eventID")){ //Add Event node
				name = obj.getString("eventName");
				eventId = obj.getString("eventID");
				nb.insertAwardEvent(name, eventId);
				nb.close();
				sendString(request, obj.toString(), 200);
			}else if(obj.length() == 2 && obj.has("categoryName") && obj.has("categoryID")){ //Add Award Category node
				name = obj.getString("categoryName");
				categoryId = obj.getString("categoryID");
				nb.insertAwardCategory(name, categoryId);
				nb.close();
				sendString(request, obj.toString(), 200);
			}
			else if(obj.length() == 2 && obj.has("eventID") && obj.has("categoryID")){ //Add event HAS an AWARD CATEGORY
				eventId = obj.getString("eventID");
				categoryId = obj.getString("categoryID");
				nb.insertHasRelationship(eventId, categoryId);
				nb.close();
				sendString(request, obj.toString(), 200);
			}
			else if(obj.length() == 3 && obj.has("actorID") && obj.has("eventID") && obj.has("year")){ //Add actor IS_NOMINATED_AT an event
				actorId = obj.getString("actorID");	//event will take place every year, so actor can be nominated every year
				eventId = obj.getString("eventID");
				year = obj.getInt("year");
				nb.insertIsNominatedRelationship(actorId, eventId, year);
				nb.close();
				sendString(request, obj.toString(), 200);
			}else if(obj.length() == 4 && obj.has("actorID") && obj.has("categoryID") && obj.has("year") && obj.has("eventID")){ //Add actor WON an award category
				eventId = obj.getString("eventID");
				actorId = obj.getString("actorID");	//event will take place every year, so actor can be nominated every year
				categoryId = obj.getString("categoryID");
				year = obj.getInt("year");
				nb.insertWONRelationship(actorId, categoryId, eventId, year);
				nb.close();
				sendString(request, obj.toString(), 200);
			}else if(obj.length() == 2 &&  obj.has("categoryID") && obj.has("movieID")) {	//Add movie IS_AWARDED for an award category
				categoryId = obj.getString("categoryID");
				movieId = obj.getString("movieID");
				nb.insertmovieIsAwardedRelationship(movieId, categoryId);
				nb.close();
				sendString(request, obj.toString(), 200);

			}
			else {
				request.sendResponseHeaders(400, -1);
				return;
			}
			//	        	

		}catch (Exception e) {

		}

	}

	private void handleGet(HttpExchange request) throws IOException {
		InputStream is = request.getRequestBody();
		String requestBody = Utils.convert(is);
		System.out.println(requestBody);
		
		try {
			JSONObject obj = new JSONObject(requestBody);
			Neo4jFilm nb = new Neo4jFilm();
			String actorId;
			String movieId;
			String eventId;
			String categoryId;
			String response;
			if(obj.length() == 2 && obj.has("movieId") && obj.has("actorId")) {
				movieId = obj.getString("movieId");
				actorId = obj.getString("actorId");
				String hasRelationship = nb.hasRelationship(movieId, actorId, request);
				System.out.println(hasRelationship);
				nb.close();
				sendString(request, hasRelationship, 200);
			}else if(obj.length() == 1 && obj.has("actorId") ) {
				System.out.println("Passed here");
				actorId = obj.getString("actorId");
				String getActor = nb.getActor(actorId, request);
				System.out.println(getActor);
				nb.close();
				sendString(request, getActor, 200);
			}else if(obj.length() == 1 && obj.has("movieId")){
				movieId = obj.getString("movieId");
				String getMovie = nb.getMovie(movieId, request);
				System.out.println(getMovie);
				nb.close();
				sendString(request, getMovie, 200);
			}else if(obj.length() == 1 && obj.has("actorID")) { //returns whether specific actor received any award from any event and number of awards the actor has received
				actorId = obj.getString("actorID");
				response = nb.getActorWonAward(actorId, request);
				nb.close();
				sendString(request, response, 200);
			}else if(obj.length() == 1 && obj.has("categoryID")) {	//get a list of actors that received specific award category
				categoryId = obj.getString("categoryID");
				response = nb.getAwardCategory(categoryId);
				nb.close();
				sendString(request, response, 200);
			}else if(obj.length() == 2 && obj.has("categoryID") && obj.has("eventID")) {	//getEventHasAward
				eventId = obj.getString("eventID");
				categoryId = obj.getString("categoryID");
				response = nb.getEventHasAward(eventId, categoryId, request);
				nb.close();
				sendString(request, response, 200);
			}else if(obj.length() == 1 && obj.has("eventID")) {		//get a list of actors who IS_NOMINATED_AT an event
				eventId = obj.getString("eventID");
				response = nb.getAwardEvent(eventId);
				nb.close();
				sendString(request, response, 200);
			}else if(obj.length() == 2 && obj.has("actorID") && obj.has("eventID")) {	//getNominated
				eventId = obj.getString("eventID");
				actorId = obj.getString("actorID");
				response = nb.getNominatedAtEvent(actorId, eventId);
				nb.close();
				sendString(request, response, 200);
			}
			
			else {
				request.sendResponseHeaders(400, -1);
				return;
			}
			
		}catch (Exception e) {

		}
	}

}
