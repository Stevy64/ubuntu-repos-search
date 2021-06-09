/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package LinuxPackageSearch;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

/**
 *
 * @author steevy
 */
public class MainVerticle {

	public static void main(String[] args) throws InterruptedException {

		Vertx vertx = Vertx.vertx();

		vertx.deployVerticle(
				new EventBusPublisherVerticle(), 
				new Handler<AsyncResult<String>>() {

			@Override
			public void handle(AsyncResult<String> event) {
				// TODO Auto-generated method stub
			}
		});

		vertx.deployVerticle(new PackageManagerSearch(), new Handler<AsyncResult<String>>() {

			@Override
			public void handle(AsyncResult<String> event) {
				// TODO Auto-generated method stub
			}
		});

	}

}
