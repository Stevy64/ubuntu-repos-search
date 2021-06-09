/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package LinuxPackageSearch;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;

/**
 *
 * @author steevy
 */
public class EventBusPublisherVerticle extends AbstractVerticle {
    
    String pkgName = "firefox";
    
	@Override
	public void start (Future<Void> startFuture) throws Exception {
        vertx.eventBus().request("linux", pkgName, ar -> {
        	  if (ar.succeeded()) {
        		  System.out.println("Package sent = " + pkgName);
        		  System.out.println("Package received back = " + ar.result().body());
        		    }
        	  else {
        		  System.out.println("Failure -- " + ar.cause());
        	  }
        	});
	}
	
	@Override
	public void stop() throws Exception {
		System.out.println("Verticle stopped on chanel : kit.applications.package.name.search");
	}
}

