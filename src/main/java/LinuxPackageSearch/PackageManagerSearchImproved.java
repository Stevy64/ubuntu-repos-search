/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package LinuxPackageSearch;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

/**
 *
 * @author steevy
 */
public class PackageManagerSearchImproved extends AbstractVerticle {
	public String ubuntuRepos = "http://archive.ubuntu.com/ubuntu/dists/";
	public String[] ubuntuCodeName = {"focal", "bionic"};
	public String[] ubuntuPkg = {"/main/i18n/Translation-fr.gz", "/universe/binary-amd64/Packages.gz"};

	@Override
	public void start(Future<Void> startFuture) {
		vertx.eventBus().consumer("linux", message -> {
			try {
				querryVerticle(message.body().toString());
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Package received message: " + message.body());
		});
	}

	// Search implementation
	public void querryVerticle(String pkgName) throws FileNotFoundException {
		WebClientOptions options = new WebClientOptions().setUserAgent(ubuntuRepos);
		WebClient client = WebClient.create(vertx, options);

		String repository = ubuntuRepos + ubuntuCodeName[0] + ubuntuPkg[0];

		// Send a HEAD request to check existence
		client
        .headAbs(ubuntuRepos + "/focal" + "/bionic/main/i18n/Translation-fr")
        .send(ar -> {
            if (ar.succeeded()) {
                // Obtain response
                HttpResponse<Buffer> response = ar.result();

                System.out.println("HEAD : Received response with status code" + response.bodyAsString());
            } else {
                System.out.println("HEAD : Something went wrong " + ar.cause().getMessage());
            }
        });

		Pattern packageNamePattern = Pattern.compile("Package: (.*)");
		Pattern descriptionPattern = Pattern.compile("Description(-fr)?: (.*)");
		JsonObject jsonObject = new JsonObject();
		JsonArray jsonArray = new JsonArray();
		
		// Send a GET request
		client.getAbs(repository).followRedirects(true).send(ar -> {
			if (ar.succeeded()) {
				// Obtain response
				HttpResponse<Buffer> response = ar.result();
				System.out.println("URL : " + repository);
				try {
					ByteArrayInputStream responseBytes = new ByteArrayInputStream(response.body().getBytes());
					GZIPInputStream responseGzip = new GZIPInputStream(responseBytes);
					InputStreamReader responseContent = new InputStreamReader(responseGzip, "UTF-8");
					BufferedReader data = new BufferedReader(responseContent);
					
					StringBuffer sb = new StringBuffer();
			        while (true) {
			            String line = data.readLine();
			            if (line == "" || line == null) break;
			            	sb.append(line).append("\n");
			        }
			        
			        String[] blocks = sb.toString().split("\n\n");
			        for (String block : blocks) {
			            //block = block.trim();
			            //System.out.println("Block : " + block);
			        	if (block.startsWith("Package") && block.contains(pkgName)) {
			        		String[] lines = block.split("\n");
			                for (String line : lines) {
			                	Matcher m = packageNamePattern.matcher(line);
								if (m.matches()) {
									jsonObject.put("Package", m.group(1));
									jsonArray.add(m.group(1));
									//System.out.println("Package : " + m.group(1));
								}
								else {
									m = descriptionPattern.matcher(line);
									if (m.matches()) {
										jsonObject.put("Description", m.group(2));
										jsonArray.add(m.group(2));
										//System.out.println("Description : " + m.group(2));
									}
								}
								}
			                }
			        	
			        	//System.out.println("JSON : " + jsonObject.encodePrettily());
			        		//System.out.println("Block : " + block.substring(block.lastIndexOf("Package"), block.lastIndexOf("Description")));
			        		//System.out.println("Block : " + block.substring(block.lastIndexOf("Description")));
//			        		jsonObject.put("Package", block.substring(block.lastIndexOf("Package")));
//			        		jsonObject.put("Description", block.substring(block.lastIndexOf("Description")));
//			        		System.out.println("JSON : " + jsonObject.encodePrettily());
//			        	Matcher m = packageNamePattern.matcher(block);
//						if (m.matches()) {
//							System.out.println("Matchers : Package....");
//							if (m.group(1).contains(pkgName)) {
//								jsonObject.put("Package", m.group(1));
//								System.out.println("Package" + m.group(1));
//							}
//						else {
//							m = descriptionPattern.matcher(block);
//							if (m.matches()) {
//								jsonObject.put("Description", m.group(2));
//								System.out.println("Description \"" + m.group(2));
//							}
//						}
//							//System.out.println("MAPING : " + jsonObject.encodePrettily());
//						}
			        } 
			        System.out.println("MAPING : " + jsonArray.encodePrettily());
//					String line;
//					while ((line = data.readLine()) != null) {
//						Matcher m = packageNamePattern.matcher(line);
//						if (m.matches()) {
//							if (m.group(1).contains(pkgName)) {
//								jsonObject.put("Package", m.group(1));
//								System.out.println("Package" + m.group(1));
//							}
//						else {
//							m = descriptionPattern.matcher(line);
//							if (m.matches()) {
//								jsonObject.put("Description", m.group(2));
//								System.out.println("Description \"" + m.group(2));
//							}
//						}
//							//System.out.println("MAPING : " + jsonObject.encodePrettily());
//						}
//						//System.out.println("MAPING : " + jsonObject.encodePrettily());
//						 //System.out.println(line);
//					}
				} catch (IOException e) {
					e.printStackTrace();
				}

				// System.out.println("GET : Received response with status code" +
				// response.bodyAsString());
			} else {
				System.out.println("GET : Something went wrong " + ar.cause().getMessage());
			}
		});

	}

}
