package client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import network.NetworkLayerJSON;
import utils.EmailUtils;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

/**
 * Simplified GUI for Mailify: register and login only.
 */
public class EmailClientGUI extends JFrame {
    private NetworkLayerJSON network;
    private JTextField userField;
    private JPasswordField passField;

    public EmailClientGUI() {
        super("Mailify - Login/Register");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(400, 200);
        setLocationRelativeTo(null);
        try {
            network = new NetworkLayerJSON(EmailUtils.HOSTNAME, EmailUtils.PORT);
            network.connect();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Connection error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        userField = new JTextField();
        passField = new JPasswordField();

        panel.add(new JLabel("Username:"));
        panel.add(userField);
        panel.add(new JLabel("Password:"));
        panel.add(passField);

        JButton loginBtn = new JButton("Login");
        JButton registerBtn = new JButton("Register");
        loginBtn.addActionListener(this::doAuth);
        registerBtn.addActionListener(this::doAuth);

        panel.add(loginBtn);
        panel.add(registerBtn);

        add(panel);
    }

    private void doAuth(ActionEvent e) {
        String cmd = ((JButton) e.getSource()).getText().equals("Login")
                ? EmailUtils.LOGIN : EmailUtils.REGISTER;
        String user = userField.getText().trim();
        String pass = new String(passField.getPassword());
        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter both username and password.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JsonObject req = new JsonObject();
        req.addProperty(EmailUtils.FIELD_COMMAND, cmd);
        req.addProperty(EmailUtils.FIELD_USERNAME, user);
        req.addProperty(EmailUtils.FIELD_PASSWORD, pass);
        network.send(req.toString());
        JsonObject resp = JsonParser.parseString(network.receive()).getAsJsonObject();
        String status = resp.get(EmailUtils.FIELD_STATUS).getAsString();
        if ((cmd.equals(EmailUtils.LOGIN) && EmailUtils.STATUS_LOGIN_SUCCESS.equals(status)) ||
                (cmd.equals(EmailUtils.REGISTER) && EmailUtils.STATUS_REGISTERED.equals(status))) {
            JOptionPane.showMessageDialog(this, cmd + " successful!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } else {
            String err = resp.has(EmailUtils.FIELD_ERROR)
                    ? resp.get(EmailUtils.FIELD_ERROR).getAsString()
                    : "Unknown error";
            JOptionPane.showMessageDialog(this, err, "Failure", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new EmailClientGUI().setVisible(true));
    }
}
