package flexi.fluid.client;

import java.util.Random;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.CssColor;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;

import flexi.fluid.shared.NavierStokesSolver;

/**
 * GWT 2.3 implementation of my fluid simulation
 * 
 * Kudos to Chad Lung for GWT Canvas tutorial: http://www.giantflyingsaucer.com/blog/?p=2338
 * 
 * @author Felix Woitzel, May 2011
 * 
 */
public class GWTFluidCanvas implements EntryPoint {
	private Canvas canvas;
	private Context2d context;
	private static final int maxParticlesNum = 10000;
	private static final int canvasHeight = 400;
	private static final int canvasWidth = 400;
	int oldMouseX = 1, oldMouseY = 1;

	private Label fpsLabel;

	private TextBox particleNum;
	private Button setParticleNum;

	private NavierStokesSolver fluidSolver;
	private double visc, diff, limitVelocity, vScale;

	private long oldMillis = System.currentTimeMillis();

	int numParticles = 250;
	Particle[] particles;
	Random rnd = new Random();

	int mouseX;
	int mouseY;

	CssColor vectorColor = CssColor.make(192, 192, 192);
	CssColor particleColor = CssColor.make(0, 0, 128);
	CssColor gridColor = CssColor.make(223, 223, 223);

	boolean showGrid = true;
	boolean showVectors = true;

	private long currentMillis;

	public void onModuleLoad() {

		final DialogBox dialogBox = new DialogBox();
		dialogBox.setModal(false);

		canvas = Canvas.createIfSupported();

		if (canvas == null) {
			dialogBox.setWidget(new Label("Sorry, your browser doesn't support the HTML5 Canvas element"));
			dialogBox.show();
			return;
		}

		RootPanel.get().add(new Label("FPS: "));
		fpsLabel = new Label();
		RootPanel.get().add(fpsLabel);

		RootPanel.get().add(new Label("number of particles (0-10000):"));
		particleNum = new TextBox();
		particleNum.setText("" + numParticles);
		RootPanel.get().add(particleNum);

		setParticleNum = new Button("OK");
		setParticleNum.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				try {
					numParticles = Integer.parseInt(particleNum.getText());
				} catch (Exception e) {
				} finally {
					numParticles = Math.max(0, Math.min(maxParticlesNum, numParticles)); // clamp to [0 - max]
					particleNum.setText("" + numParticles);
				}
			}
		});
		RootPanel.get().add(setParticleNum);

		final CheckBox showGridBtn = new CheckBox("show grid");
		showGridBtn.setValue(true);
		showGridBtn.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				showGrid = showGridBtn.getValue();
			}
		});
		RootPanel.get().add(showGridBtn);
		
		final CheckBox showVectorsBtn = new CheckBox("show vectors");
		showVectorsBtn.setValue(true);
		showVectorsBtn.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				showVectors = showVectorsBtn.getValue();
			}
		});
		RootPanel.get().add(showVectorsBtn);

		visc = 0.001;
		diff = 0.3;
		fluidSolver = new NavierStokesSolver();

		limitVelocity = 200;
		vScale = 50;

		initHandlers();

		canvas.setStyleName("mainCanvas");
		canvas.setWidth(canvasWidth + "px");
		canvas.setCoordinateSpaceWidth(canvasWidth);

		canvas.setHeight(canvasHeight + "px");
		canvas.setCoordinateSpaceHeight(canvasHeight);

		// RootPanel.get().add(canvas);

		dialogBox.setText("GWT Canvas Fluid Simulation");
		dialogBox.setWidget(canvas);
		dialogBox.setPopupPosition(75, 175);
		dialogBox.show();

		context = canvas.getContext2d();

		final Timer drawLoopTimer = new Timer() {
			@Override
			public void run() {
				draw();
			}
		};
		drawLoopTimer.scheduleRepeating(7); // optimistic? - the IE9 delivered sweet 70fps and that was limited by my screen :)

		particles = new Particle[maxParticlesNum];
		initParticles();
	}

	private void initParticles() {
		for (int i = 0; i < maxParticlesNum - 1; i++) {
			particles[i] = new Particle();
			particles[i].x = rnd.nextFloat() * canvasWidth;
			particles[i].y = rnd.nextFloat() * canvasHeight;
		}
	}

	public class Particle {
		public double x;
		public double y;
	}

	// XXX: somehow the event handling slows down the animation noticeably
	void initHandlers() {
		canvas.addMouseMoveHandler(new MouseMoveHandler() {
			public void onMouseMove(MouseMoveEvent event) {
				event.stopPropagation(); // this speeds up the animation
				mouseX = event.getRelativeX(canvas.getElement());
				mouseY = event.getRelativeY(canvas.getElement());
			}
		});

		// canvas.addMouseOutHandler(new MouseOutHandler() {
		// public void onMouseOut(MouseOutEvent event) {
		// event.stopPropagation();
		// mouseX = -200;
		// mouseY = -200;
		// }
		// });

		// canvas.addTouchMoveHandler(new TouchMoveHandler() {
		// public void onTouchMove(TouchMoveEvent event) {
		// event.preventDefault();
		// if (event.getTouches().length() > 0) {
		// Touch touch = event.getTouches().get(0);
		// mouseX = touch.getRelativeX(canvas.getElement());
		// mouseY = touch.getRelativeY(canvas.getElement());
		// }
		// event.preventDefault();
		// }
		// });
		//
		// canvas.addTouchEndHandler(new TouchEndHandler() {
		// public void onTouchEnd(TouchEndEvent event) {
		// event.preventDefault();
		// mouseX = -200;
		// mouseY = -200;
		// }
		// });
		//
		// canvas.addGestureStartHandler(new GestureStartHandler() {
		// public void onGestureStart(GestureStartEvent event) {
		// event.preventDefault();
		// }
		// });
	}

	private void draw() {
		currentMillis = System.currentTimeMillis();
		double dt = (currentMillis - oldMillis) * 0.001;
		oldMillis = currentMillis;
		fpsLabel.setText("" + Math.floor(1 / dt));
		handleMouseMotion();

		fluidSolver.tick(dt, visc, diff);
		vScale = dt * canvasWidth * 1.25;
		context.clearRect(0, 0, canvasWidth, canvasHeight); // sets transparency to 0%

		if (showGrid) {
			drawGrid();
		}

		if (showVectors) {
			drawVectors();
		}

		drawParticles();
	}

	private void drawParticles() {
		int n = NavierStokesSolver.N;
		double cellHeight = canvasHeight / (double) n;
		double cellWidth = canvasWidth / (double) n;

		// context.setStrokeStyle(blue);
		context.setFillStyle(particleColor);
		// context.beginPath();

		for (int i = 0; i < numParticles; i++) {
			Particle p = particles[i];
			if (p != null) {
				int cellX = (int) Math.floor(p.x / (double) cellWidth);
				int cellY = (int) Math.floor(p.y / (double) cellHeight);
				double dx = fluidSolver.getDx(cellX, cellY);
				double dy = fluidSolver.getDy(cellX, cellY);

				double lX = p.x - cellX * cellWidth - cellWidth / 2;
				double lY = p.y - cellY * cellHeight - cellHeight / 2;

				int v, h, vf, hf;

				if (lX > 0) {
					v = Math.min(n, cellX + 1);
					vf = 1;
				} else {
					v = Math.min(n, cellX - 1);
					vf = -1;
				}

				if (lY > 0) {
					h = Math.min(n, cellY + 1);
					hf = 1;
				} else {
					h = Math.min(n, cellY - 1);
					hf = -1;
				}

				double dxv = fluidSolver.getDx(v, cellY);
				double dxh = fluidSolver.getDx(cellX, h);
				double dxvh = fluidSolver.getDx(v, h);

				double dyv = fluidSolver.getDy(v, cellY);
				double dyh = fluidSolver.getDy(cellX, h);
				double dyvh = fluidSolver.getDy(v, h);

				dx = lerp(lerp(dx, dxv, hf * lY / (double) cellWidth), lerp(dxh, dxvh, hf * lY / (double) cellWidth), vf * lX / (double) cellHeight);

				dy = lerp(lerp(dy, dyv, hf * lY / (double) cellWidth), lerp(dyh, dyvh, hf * lY / (double) cellWidth), vf * lX / (double) cellHeight);

				p.x += dx * vScale;
				p.y += dy * vScale;

				if (p.x < 0 || p.x >= canvasWidth) {
					p.x = rnd.nextDouble() * canvasWidth;
				}
				if (p.y < 0 || p.y >= canvasHeight) {
					p.y = rnd.nextDouble() * canvasHeight;
				}

				// XXX: somehow paint on canvas

				// set((int) p.x, (int) p.y, c);
				// context.moveTo(p.x, p.y);
				// context.lineTo(p.x + 1, p.y);
				// context.lineTo(p.x + 1, p.y + 1);
				// context.lineTo(p.x, p.y + 1);

				context.fillRect(p.x - 1, p.y - 1, 2, 2);

			}
		}
		// context.closePath();
		// context.fill();
		// context.stroke();
	}

	private void drawVectors() {
		double cx, cy, dx, dy;
		context.setStrokeStyle(vectorColor);
		context.beginPath();
		int n = NavierStokesSolver.N;
		double n_inverse = 1 / (double) n;
		for (int y = 0; y < n; y++) {
			cy = canvasHeight * (y + 0.5) * n_inverse;
			for (int x = 0; x < n; x++) {
				cx = canvasWidth * (x + 0.5) * n_inverse;
				dx = fluidSolver.getDx(x, y) * vScale * 2;
				dy = fluidSolver.getDy(x, y) * vScale * 2;
				context.moveTo(cx, cy);
				context.lineTo(cx + dx, cy + dy);
				// context.lineTo(cx + 10, cy + 10);
			}
			// context.moveTo(canvasWidth * y * n_inverse, canvasHeight * y * n_inverse);
			// context.lineTo(canvasWidth * y * n_inverse + 10, canvasHeight * y * n_inverse + 10);
		}
		context.closePath();
		context.stroke();
	}

	private void drawGrid() {
		context.setStrokeStyle(gridColor);
		context.beginPath();
		int n = NavierStokesSolver.N;
		for (int d = 1; d < n; d++) {
			context.moveTo(canvasWidth * d / n, 0);
			context.lineTo(canvasWidth * d / n, canvasHeight);
			context.moveTo(0, canvasHeight * d / n);
			context.lineTo(canvasWidth, canvasHeight * d / n);
		}
		context.closePath();
		context.stroke();
	}

	private void handleMouseMotion() {
		mouseX = Math.max(1, mouseX);
		mouseY = Math.max(1, mouseY);

		int n = NavierStokesSolver.N;
		double cellHeight = (double) canvasHeight / (double) n;
		double cellWidth = (double) canvasWidth / (double) n;

		double mouseDx = mouseX - oldMouseX;
		double mouseDy = mouseY - oldMouseY;
		int cellX = (int) Math.floor(mouseX / cellWidth);
		int cellY = (int) Math.floor(mouseY / cellHeight);

		mouseDx = (Math.abs(mouseDx) > limitVelocity) ? Math.signum(mouseDx) * limitVelocity : mouseDx;
		mouseDy = (Math.abs(mouseDy) > limitVelocity) ? Math.signum(mouseDy) * limitVelocity : mouseDy;

		fluidSolver.applyForce(cellX, cellY, mouseDx, mouseDy);

		oldMouseX = mouseX;
		oldMouseY = mouseY;
	}

	private double lerp(double v1, double v2, double i) {
		return v2 * i + v1 * (1 - i);
	}
}
