package ca.yorku.eecs;

import static org.neo4j.driver.v1.Values.parameters;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import org.json.*;
import org.neo4j.driver.v1.*;


import com.sun.net.httpserver.*;


public class Neo4jFilm {


	private Driver driver;
	private String uriDb;


	public Neo4jFilm() {
		uriDb = "bolt://localhost:7687"; 
		Config config = Config.builder().withoutEncryption().build();
		driver = GraphDatabase.driver(uriDb, AuthTokens.basic("neo4j","123456"), config);
	}

	public boolean insertActor(String actor, String id) {
		try (Session session = driver.session()){
			StatementResult sr = session.writeTransaction(tx -> tx.run("MERGE (a:actor {id: $y})\n" + "ON CREATE SET a.Name = $x\n",
					parameters("y", id, "x", actor)));
			session.close();
			if(sr.hasNext() == false) {
				return false;
			}else {
				System.out.println(actor + " is added successfully!" );
			}
			
		}
		return true;
	}


	public void insertMovie(String movie, String id) {
		try (Session session = driver.session()){
			session.writeTransaction(tx -> tx.run("MERGE (m:movie {id: $y})\n" + "ON CREATE SET m.Name = $x\n",
					parameters( "y", id, "x", movie)));
			session.close();
		}
	}

	public void insertActedIn(String actorId, String movieId) {
		try (Session session = driver.session()){
			session.writeTransaction(tx -> tx.run("MATCH (a:actor {id: $x}),"
					+ "(m:movie {id: $y})\n" + 
					"MERGE (a)-[r:ACTED_IN]->(m)\n" + 
					"RETURN r", parameters("x", actorId, "y", movieId)));
			session.close();
		}
	}
	//Syed
	public void insertAwardEvent(String name, String id) {
		try (Session session = driver.session()){
			session.writeTransaction(tx -> tx.run("MERGE (e:event {id: $y})\n" + "ON CREATE SET e.Name = $x\n" , 
					parameters("x", name, "y", id)));
			session.close();
		}
	}
	//Bailey
	public void insertAwardCategory(String name, String categoryId) {
		try (Session session = driver.session()){
			session.writeTransaction(tx -> tx.run("MERGE (c:category {id: $y})\n" + "ON CREATE SET c.Name = $x\n" , 
					parameters("x", name, "y", categoryId)));
			session.close();
		}
	}
	public void insertHasRelationship(String eventId, String categoryId) {	//event HAS an AWARD CATEGORY
		try (Session session = driver.session()){
			session.writeTransaction(tx -> tx.run("MATCH (e:event {id: $x}),"
					+ "(c:category {id: $y})\n" + 
					"MERGE (e)-[r:HAS]->(c)\n" + 
					"RETURN r", parameters("x", eventId, "y", categoryId)));
			session.close();
		}
	}
	public void insertIsNominatedRelationship(String actorId, String eventId, int year) {
		try (Session session = driver.session()){
			session.writeTransaction(tx -> tx.run("MATCH (a:actor {id: $x}),"
					+ "(e:event {id: $y})\n" + 
					"MERGE (a)-[r:IS_NOMINATED_AT {year: $z}]->(e)\n" + 
					"RETURN r", parameters("x", actorId, "y", eventId, "z", year)));
			session.close();
		}
	}
	public void insertWONRelationship(String actorId, String categoryId, String eventId, int year) {
		try (Session session = driver.session()){
			session.writeTransaction(tx -> tx.run("MATCH (a:actor {id: $x}),"
					+ "(c:category {id: $y})," + "(e:event {id: $m})\n" + 
					"WHERE EXISTS((a)-[:IS_NOMINATED_AT {year: $z}]->(e)) AND (e)-[:HAS]->(c)\n" + //need to get nomination at the event in the same year
					"MERGE (a)-[r:WON {year: $z}]->(c)\n" + 					//then the actor won this category
					"RETURN r", parameters("x", actorId, "y", categoryId, "z", year, "m", eventId)));
			session.close();
		}
	}
	public void insertmovieIsAwardedRelationship(String movieId, String categoryId) {		//Movie IS_AWARDED for an award category
		try (Session session = driver.session()){
			session.writeTransaction(tx -> tx.run("MATCH (m:movie {id: $x}),"
					+ "(c:category {id: $y})\n" + 
					"MERGE (e)-[r:IS_AWARDED]->(c)\n" + 
					"RETURN r", parameters("x", movieId, "y", categoryId)));
			session.close();
		}
		
		
	}
	public String getActorWonAward(String actorId, HttpExchange request ) {
		try (Session session = driver.session()){
			StatementResult sr = session.run("MATCH (a:actor {id: $x}), (c:category)\n" + "WHERE EXISTS ((a)-[:WON]->(c))\n" +  "RETURN count(c) AS awardsTotal, a.id AS actorID", parameters("x", actorId));

			JSONObject obj = new JSONObject();
			boolean checkAward = true;
			if(sr.hasNext() == false) {	//base case. If the actor doesn't win any award, set # of award as 0 and hasAward to false
				try {
					obj.put("actorID", actorId);
					obj.put("actorHasAward", false);
					obj.put("awardsTotal", 0);
					try {
						request.sendResponseHeaders(400, -1);
						return "";
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

					
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					try {
						request.sendResponseHeaders(400, -1);
						return "";
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					
				}
				
			}
			while(sr.hasNext()) {
				Map<String, Object> row = sr.next().asMap();
				for (Entry<String,Object> column : row.entrySet() )
				{
					
					if(column.getKey().equals("actorID")) {
						try {
							obj.put(column.getKey(), column.getValue());
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							try {
								request.sendResponseHeaders(400, -1);
								return "";
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						}
					}
					if(column.getKey().equals("awardsTotal")) {
						try {
							obj.put(column.getKey(), column.getValue());
							if(column.getValue().equals(0)) {
								checkAward = false;
								obj.put("actorHasAward", checkAward);
							}else {
								obj.put("actorHasAward", checkAward);
							}
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							try {
								request.sendResponseHeaders(400, -1);
								return "";
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						}
					}
				}

			}
		
			return obj.toString();
		}
	}

	public String getActor(String actorId, HttpExchange request ) {
		try (Session session = driver.session()){

			StatementResult sr = session.run("MATCH (a:actor), (m:movie), (a)-[r:ACTED_IN]->(m)\n" + "WHERE a.id = $x\n" +  "RETURN a.id AS actorId, a.Name AS name, collect(m.id) AS movies", parameters("x", actorId));
			StatementResult sr2 = session.run("MATCH (a:actor {id: $y})\nRETURN a.id AS id", parameters("y", actorId));
			JSONObject obj = new JSONObject();
			if(sr2.hasNext() != true) {
				try {
					request.sendResponseHeaders(400,-1);	//actor isn't in the database
					
					return obj.toString();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}else {
				while(sr.hasNext()) {
					Map<String, Object> row = sr.next().asMap();
					for (Entry<String,Object> column : row.entrySet() )
					{
						
						if(column.getKey().equals("actorId")) {
							try {
								obj.put(column.getKey(), column.getValue());
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						if(column.getKey().equals("name")) {
							try {
								obj.put(column.getKey(), column.getValue());
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						
						if(column.getKey().equals("movies")) {
							try {
								obj.put(column.getKey(), column.getValue());
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
//					
					}

				}
			}
			
//			
			
			return obj.toString();

		}
	}
	public String getMovie(String movieId, HttpExchange request ) {
		try (Session session = driver.session()){

			StatementResult sr = session.run("MATCH (a:actor), (m:movie), (a)-[r:ACTED_IN]->(m)\n" + "WHERE m.id = $x\n" +  "RETURN m.id AS movieId, m.Name AS name, collect(a.id) AS actors", parameters("x", movieId));
//			StatementResult sr2 = session.run("MATCH (m:movie {id: $y})\nRETURN m", parameters("y", movieId));

			JSONObject obj = new JSONObject();
//		
				while(sr.hasNext()) {
					Map<String, Object> row = sr.next().asMap();
					for (Entry<String,Object> column : row.entrySet() )
					{
						if(column.getKey().equals("movieId")) {
							try {
								obj.put(column.getKey(), column.getValue());
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						if(column.getKey().equals("name")) {
							try {
								obj.put(column.getKey(), column.getValue());
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						
						if(column.getKey().equals("actors")) {
							try {
								obj.put(column.getKey(), column.getValue());
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				}			
			return obj.toString();

		}
	}
	public String hasRelationship(String movieId, String actorId, HttpExchange request ) {	//get hasRelationship
		try (Session session = driver.session()){

			StatementResult sr = session.run("MATCH (a:actor {id: $y}), (m:movie {id: $x})\n" + "RETURN EXISTS((a)-[:ACTED_IN]->(m))", parameters("x", movieId, "y", actorId));	
			JSONObject obj = new JSONObject();
			try {
				obj.put("actorId", actorId);
				obj.put("movieId", movieId);
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			if(sr.hasNext() == false) {
				try {
					obj.put("hasRelationship", false);
					try {
						request.sendResponseHeaders(400, -1);
						return "";
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			while(sr.hasNext()) {
				boolean r = sr.next().get(0).asBoolean();
				try {
					obj.put("hasRelationship", r);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return obj.toString();

		}
	}
	public String getEventHasAward (String eventId, String categoryId,  HttpExchange request ) {
		try (Session session = driver.session()){

			StatementResult sr = session.run("MATCH (e:event {id: $x}), (c:category {id: $y})\n" + "RETURN EXISTS((e)-[:HAS]->(c))", parameters("x", eventId, "y",categoryId));	
			JSONObject obj = new JSONObject();
			try {
				obj.put("eventID", eventId);
				obj.put("categoryID", categoryId);
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			if(sr.hasNext() == false) {
				try {
					obj.put("eventHasAward", false);
					try {
						request.sendResponseHeaders(400, -1);
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			while(sr.hasNext()) {
				boolean r = sr.next().get(0).asBoolean();
				try {
					obj.put("eventHasAward", r);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return obj.toString();

		}
	}
	

	public String getAwardCategory(String categoryId) {
		try (Session session = driver.session()){

			StatementResult sr = session.run("MATCH(a:actor), (c:category {id: $x})\n" + "WHERE (a)-[:WON]->(c)\n" +  "RETURN c.id AS categoryID, c.Name AS name, collect(a.id) AS actors", parameters("x", categoryId));
			JSONObject obj = new JSONObject();
			while(sr.hasNext()) {
				Map<String, Object> row = sr.next().asMap();
				for (Entry<String,Object> column : row.entrySet() )
				{
					if(column.getKey().equals("categoryID")) {
						try {
							obj.put(column.getKey(), column.getValue());
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					if(column.getKey().equals("name")) {
						try {
							obj.put(column.getKey(), column.getValue());
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					
					if(column.getKey().equals("actors")) {
						try {
							obj.put(column.getKey(), column.getValue());
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
			return obj.toString();

		}
	}
	public String getAwardEvent(String eventId) {
		try (Session session = driver.session()){

			StatementResult sr = session.run("MATCH(a:actor), (e:event {id: $x})\n"+ "WHERE EXISTS ((a)-[:IS_NOMINATED_AT]->(e))\n" +  "RETURN e.id AS eventID, e.Name AS eventName,collect(a.id) AS nominated_actors", parameters("x", eventId));
			JSONObject obj = new JSONObject();
			while(sr.hasNext()) {
				Map<String, Object> row = sr.next().asMap();
				for (Entry<String,Object> column : row.entrySet() )
				{
					if(column.getKey().equals("eventName")) {
						try {
							obj.put(column.getKey(), column.getValue());
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					if(column.getKey().equals("eventID")) {
						try {
							obj.put(column.getKey(), column.getValue());
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					
					if(column.getKey().equals("nominated_actors")) {
						try {
							obj.put(column.getKey(), column.getValue());
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
			return obj.toString();

		}
	}
	
	public String getNominatedAtEvent(String actorId, String eventId) {
		try (Session session = driver.session()){

			StatementResult sr = session.run("MATCH (a:actor {id: $x}), (e:event {id: $y})\n" + "RETURN EXISTS((a)-[:IS_NOMINATED_AT]->(e))", parameters("y", eventId, "x", actorId));	
			JSONObject obj = new JSONObject();
			try {
				obj.put("actorID", actorId);
				obj.put("eventID", eventId);
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			if(sr.hasNext() == false) {
				try {
					obj.put("nominated", false);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			while(sr.hasNext()) {
				boolean r = sr.next().get(0).asBoolean();
				try {
					obj.put("nominated", r);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return obj.toString();

		}
		
	}
	public void close() {
		driver.close();
	}
}
