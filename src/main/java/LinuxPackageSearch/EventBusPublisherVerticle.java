/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package LinuxPackageSearch;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

/**
 *
 * @author steevy
 */
public class EventBusPublisherVerticle extends AbstractVerticle {

	private static String UBUNTU_REPOS = "firefox";
	private static String[] UBUNTU_CODE_NAME = { "focal", "bionic" };

	@Override
	public void start(Future<Void> startFuture) throws Exception {
		vertx.eventBus().request("kit.applications.ubuntu.package.search",
				new JsonObject().put("Package", UBUNTU_REPOS).put("UbuntuCodeName", UBUNTU_CODE_NAME[0]), ar -> {
					if (ar.succeeded()) {
						// System.out.println("pkgName sent = " + );
						System.out.println("Package received back = " + ar.result().body());
					} else {
						System.out.println("Failure -- " + ar.cause());
					}
				});
	}

	@Override
	public void stop() throws Exception {
		System.out.println("Verticle stopped on chanel : kit.applications.package.name.search");
	}
}
