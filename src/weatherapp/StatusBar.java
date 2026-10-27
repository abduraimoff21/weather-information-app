package weatherapp;

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

/** The message line under the search box: progress and results in blue, errors in red. */
class StatusBar extends RoundedPanel {

    private final JLabel message = Ui.label("", Font.PLAIN, 15, Ui.TEXT);

    StatusBar() {
        super(new BorderLayout(), Ui.CARD);
        setBorder(BorderFactory.createEmptyBorder(9, 16, 9, 16));
        add(message, BorderLayout.CENTER);
    }

    void showInfo(String text) {
        message.setText(text);
        setFill(Ui.CARD);
    }

    void showError(WeatherException problem) {
        message.setText(problem.type().title() + ": " + problem.getMessage());
        setFill(Ui.ERROR);
    }

    /** Reports a bug (an exception nobody expected) without exposing technical details. */
    void showUnexpectedError() {
        message.setText("Something went wrong inside the app. Please try the search again.");
        setFill(Ui.ERROR);
    }

    String text() {
        return message.getText();
    }
}
