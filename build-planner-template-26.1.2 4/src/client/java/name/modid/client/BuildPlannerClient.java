package name.modid.client;

import name.modid.client.screen.BuildPlannerDashboard;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.lwjgl.glfw.GLFW;

public class BuildPlannerClient implements ClientModInitializer {

	// Tracks whether B was held last tick so we only fire once per press
	private boolean keyWasDown = false;

	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			// Don't intercept B while a screen is already open or game not ready
			if (client.screen != null || client.getWindow() == null) {
				keyWasDown = false;
				return;
			}
			long windowHandle = client.getWindow().handle();
			boolean keyIsDown = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_B) == GLFW.GLFW_PRESS;
			if (keyIsDown && !keyWasDown) {
				client.setScreen(new BuildPlannerDashboard(null));
			}
			keyWasDown = keyIsDown;
		});
	}
}