import javafx.animation.*;
import javafx.application.Application;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class App extends Application {

    private static final int TILE_SIZE = 40;
    private static final int W = 800;
    private static final int H = 800;

    private static final int X_TILES = W / TILE_SIZE;
    private static final int Y_TILES = H / TILE_SIZE;

    private Tile[][] grid = new Tile[X_TILES][Y_TILES];
    private Scene scene;
    private Thread colorThread;
    private String level;

    private Parent createContent() {
        chooseLevel();

        Pane root = new Pane();
        root.setPrefSize(W, H);

        for (int y = 0; y < Y_TILES; y++) {
            for (int x = 0; x < X_TILES; x++) {
                Tile tile = new Tile(x, y, Math.random() < 0.2);
                grid[x][y] = tile;
                root.getChildren().add(tile);
            }
        }

        for (int y = 0; y < Y_TILES; y++) {
            for (int x = 0; x < X_TILES; x++) {
                Tile tile = grid[x][y];

                if (tile.hasBomb)
                    continue;

                long bombs = getNeighbors(tile).stream().filter(t -> t.hasBomb).count();

                if (bombs > 0)
                    tile.text.setText(String.valueOf(bombs));
            }
        }

        if (colorThread == null && level.equals("Hard")) {
            colorThread = new Thread(() -> {
                while (true) {
                    for (int i = 0; i < 1; i++) {
                        Random random = new Random();
                        int x = random.nextInt(W / TILE_SIZE);
                        int y = random.nextInt(H / TILE_SIZE);

                        Timeline timeline = new Timeline();
                        KeyValue colorValue = new KeyValue(grid[x][y].getCell().fillProperty(), Color.GREY);
                        KeyValue textValue = new KeyValue(grid[x][y].getText().textProperty(), " ");
                        KeyValue openedValue = new KeyValue(grid[x][y].openProperty(), false);
                        KeyFrame colorFrame = new KeyFrame(Duration.millis(2000), colorValue);
                        KeyFrame textFrame = new KeyFrame(Duration.millis(2000), textValue);
                        KeyFrame openedFrame = new KeyFrame(Duration.millis(2000), openedValue);
                        timeline.getKeyFrames().addAll(colorFrame, textFrame, openedFrame);
                        timeline.play();
                    }

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });

            colorThread.start();
        }

        return root;
    }

    private void chooseLevel() {
        List<String> choices = new ArrayList<>();
        choices.add("Easy");
        choices.add("Normal");
        choices.add("Hard");

        ChoiceDialog<String> dialog = new ChoiceDialog<>(level == null ? "Easy" : level, choices);
        dialog.setTitle("Start");
        dialog.setHeaderText("Easy, normal or hard?");
        dialog.setContentText("Choose your level:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(level -> this.level = level);
    }

    private List<Tile> getNeighbors(Tile tile) {
        List<Tile> neighbors = new ArrayList<>();

        int[] points = new int[]{
                -1, -1,
                -1, 0,
                -1, 1,
                0, -1,
                0, 1,
                1, -1,
                1, 0,
                1, 1
        };

        for (int i = 0; i < points.length; i++) {
            int dx = points[i];
            int dy = points[++i];

            int newX = tile.x + dx;
            int newY = tile.y + dy;

            if (newX >= 0 && newX < X_TILES
                    && newY >= 0 && newY < Y_TILES) {
                neighbors.add(grid[newX][newY]);
            }
        }

        return neighbors;
    }

    private class Tile extends StackPane {
        private int x, y;
        private boolean hasBomb;
        private BooleanProperty open = new SimpleBooleanProperty(false);

        private Text text = new Text();
        private Rectangle border = new Rectangle(TILE_SIZE - 2, TILE_SIZE - 2);
        private FadeTransition transition = new FadeTransition(Duration.millis(3000), text);
        private RotateTransition rotateTransition = new RotateTransition(Duration.seconds(3), text);


        public Tile(int x, int y, boolean hasBomb) {
            this.x = x;
            this.y = y;
            this.hasBomb = hasBomb;

            border.setStroke(Color.CYAN);
            border.setFill(Color.GRAY);

            text.setFont(Font.font(18));
            text.setText(hasBomb ? "X" : "");
            text.setVisible(false);

            getChildren().addAll(border, text);

            setTranslateX(x * TILE_SIZE);
            setTranslateY(y * TILE_SIZE);

            setOnMouseClicked(e -> open());
        }

        public void open() {
            if (open.getValue())
                return;

            if (hasBomb) {
                System.out.println("Game Over");
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Game over");
                alert.setHeaderText("Look, you lose!");
                alert.setContentText("But you can try again!");
                alert.showAndWait();

                scene.setRoot(createContent());
                return;
            }

            open.setValue(true);
            text.setVisible(true);
            border.setFill(Color.CYAN);

            transition.setDelay(Duration.millis(new Random().nextInt(4000)));
            transition.setFromValue(0.4);
            transition.setToValue(0);
            transition.setCycleCount(Timeline.INDEFINITE);
            transition.setAutoReverse(true);
            transition.play();

            if (level.equals("Normal") || level.equals("Hard")) {
                rotateTransition.setDelay(Duration.millis(new Random().nextInt(4000)));
                rotateTransition.setFromAngle(0);
                rotateTransition.setToAngle(360);
                rotateTransition.setCycleCount(Timeline.INDEFINITE);
                rotateTransition.play();
            }

            if (text.getText().isEmpty()) {
                getNeighbors(this).forEach(Tile::open);
            }
        }

        public Rectangle getCell() {
            return border;
        }

        public Text getText() {
            return text;
        }

        public boolean getOpen() {
            return open.get();
        }

        public BooleanProperty openProperty() {
            return open;
        }
    }

    @Override
    public void start(Stage stage) throws Exception {
        scene = new Scene(createContent());
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }


}