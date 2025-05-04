package client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import network.NetworkLayerJSON;
import utils.EmailUtils;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

/**
 * GUI email client with login/register and post-login menu options.
 * Supports sending, inbox view (with search & read), and sent view.
 */
public class EmailClientGUI extends JFrame {
    private NetworkLayerJSON network;
    private JTextField userField;
    private JPasswordField passField;

    /**
     * Constructor: initialize frame and network, show login.
     */
    public EmailClientGUI() {
        super("Mailify - Login/Register");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        connectNetwork();
        showLoginPanel();
    }

    /** Establish TCP connection using NetworkLayerJSON. */
    private void connectNetwork() {
        try {
            network = new NetworkLayerJSON(EmailUtils.HOSTNAME, EmailUtils.PORT);
            network.connect();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Connection error: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    /** Show login/register UI. */
    private void showLoginPanel() {
        setTitle("Mailify - Login/Register");
        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        userField = new JTextField();
        passField = new JPasswordField();
        panel.add(new JLabel("Username:")); panel.add(userField);
        panel.add(new JLabel("Password:")); panel.add(passField);
        JButton loginBtn = new JButton("Login");
        JButton registerBtn = new JButton("Register");
        loginBtn.addActionListener(this::doAuth);
        registerBtn.addActionListener(this::doAuth);
        panel.add(loginBtn); panel.add(registerBtn);
        setContentPane(panel);
        pack(); setLocationRelativeTo(null);
    }

    /** Perform login or register, then show main menu. */
    private void doAuth(ActionEvent e) {
        String cmd = ((JButton)e.getSource()).getText().equals("Login")
                ? EmailUtils.LOGIN : EmailUtils.REGISTER;
        String user = userField.getText().trim();
        String pass = new String(passField.getPassword());
        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Enter both username and password.",
                    "Warning",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        JsonObject req = new JsonObject();
        req.addProperty(EmailUtils.FIELD_COMMAND, cmd);
        req.addProperty(EmailUtils.FIELD_USERNAME, user);
        req.addProperty(EmailUtils.FIELD_PASSWORD, pass);
        network.send(req.toString());
        JsonObject resp = JsonParser.parseString(network.receive()).getAsJsonObject();
        String status = resp.get(EmailUtils.FIELD_STATUS).getAsString();
        boolean success = (cmd.equals(EmailUtils.LOGIN) && EmailUtils.STATUS_LOGIN_SUCCESS.equals(status)) ||
                (cmd.equals(EmailUtils.REGISTER) && EmailUtils.STATUS_REGISTERED.equals(status));
        if (success) {
            JOptionPane.showMessageDialog(this,
                    cmd + " successful!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            showMainMenuPanel();
        } else {
            String err = resp.has(EmailUtils.FIELD_ERROR)
                    ? resp.get(EmailUtils.FIELD_ERROR).getAsString()
                    : "Unknown error";
            JOptionPane.showMessageDialog(this,
                    err,
                    "Failure",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Show main menu: Send, Inbox, Sent, Logout. */
    private void showMainMenuPanel() {
        setTitle("Mailify - Main Menu");
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        JButton sendBtn = new JButton("Send Email");
        JButton inboxBtn = new JButton("Check Inbox");
        JButton sentBtn = new JButton("Check Sent");
        JButton logoutBtn = new JButton("Logout");
        sendBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        inboxBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        sentBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        logoutBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        sendBtn.addActionListener(ev -> showSendEmailPanel());
        inboxBtn.addActionListener(ev -> showInboxPanel());
        sentBtn.addActionListener(ev -> showSentPanel());
        logoutBtn.addActionListener(ev -> showLoginPanel());
        panel.add(sendBtn);
        panel.add(Box.createRigidArea(new Dimension(0,10)));
        panel.add(inboxBtn);
        panel.add(Box.createRigidArea(new Dimension(0,10)));
        panel.add(sentBtn);
        panel.add(Box.createRigidArea(new Dimension(0,10)));
        panel.add(logoutBtn);
        setContentPane(panel);
        pack(); setLocationRelativeTo(null);
    }

    /** Show panel to send an email; includes Back button. */
    private void showSendEmailPanel() {
        setTitle("Mailify - Send Email");
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5,5,5,5);
        c.fill = GridBagConstraints.HORIZONTAL;
        JTextField toField = new JTextField(20);
        JTextField subjectField = new JTextField(20);
        JTextArea bodyArea = new JTextArea(5,20);
        c.gridx=0; c.gridy=0; panel.add(new JLabel("To:"),c);
        c.gridx=1; panel.add(toField,c);
        c.gridx=0; c.gridy=1; panel.add(new JLabel("Subject:"),c);
        c.gridx=1; panel.add(subjectField,c);
        c.gridx=0; c.gridy=2; c.gridwidth=2; panel.add(new JScrollPane(bodyArea),c);
        c.gridy=3; c.gridwidth=1; c.gridx=0;
        JButton sendBtn = new JButton("Send");
        sendBtn.addActionListener(ev -> sendEmailAction(toField, subjectField, bodyArea));
        panel.add(sendBtn, c);
        c.gridx=1;
        JButton backBtn = new JButton("Back");
        backBtn.addActionListener(ev -> showMainMenuPanel());
        panel.add(backBtn, c);
        setContentPane(panel);
        pack(); setLocationRelativeTo(null);
    }

    /** Action listener for sending email. */
    private void sendEmailAction(JTextField toField, JTextField subjectField, JTextArea bodyArea) {
        String recipient = toField.getText().trim();
        String subject = subjectField.getText().trim();
        String body = bodyArea.getText().trim();
        if (recipient.isEmpty() || subject.isEmpty() || body.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "All fields required.",
                    "Warning",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        JsonObject req = new JsonObject();
        req.addProperty(EmailUtils.FIELD_COMMAND, EmailUtils.SEND);
        req.addProperty(EmailUtils.FIELD_RECIPIENT, recipient);
        req.addProperty(EmailUtils.FIELD_SUBJECT, subject);
        req.addProperty(EmailUtils.FIELD_BODY, body);
        network.send(req.toString());
        JsonObject resp = JsonParser.parseString(network.receive()).getAsJsonObject();
        String status = resp.get(EmailUtils.FIELD_STATUS).getAsString();
        if (EmailUtils.STATUS_SENT.equals(status)) {
            JOptionPane.showMessageDialog(this,
                    "Email sent!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            toField.setText(""); subjectField.setText(""); bodyArea.setText("");
        } else {
            String err = resp.has(EmailUtils.FIELD_ERROR)
                    ? resp.get(EmailUtils.FIELD_ERROR).getAsString()
                    : "Send failed";
            JOptionPane.showMessageDialog(this,
                    err,
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Show inbox table with Read, Search, and Back buttons. */
    private void showInboxPanel() {
        setTitle("Mailify - Inbox");
        DefaultTableModel model = new DefaultTableModel(new Object[]{"ID","From","Subject","Date"}, 0);
        JTable table = new JTable(model);
        loadInboxData(model, EmailUtils.LIST_INBOX, null);
        JButton readBtn = new JButton("Read");
        JButton searchBtn = new JButton("Search");
        JButton backBtn = new JButton("Back");
        readBtn.addActionListener(e -> {
            int row = table.getSelectedRow(); if (row!=-1) readEmailById((int)model.getValueAt(row,0));
        });
        searchBtn.addActionListener(e -> {
            String term = JOptionPane.showInputDialog(this,"Enter search term:");
            if(term!=null) loadInboxData(model, EmailUtils.SEARCH_INBOX, term.trim());
        });
        backBtn.addActionListener(e -> showMainMenuPanel());
        JPanel btnPanel = new JPanel(); btnPanel.add(readBtn); btnPanel.add(searchBtn); btnPanel.add(backBtn);
        JPanel panel = new JPanel(new BorderLayout()); panel.add(new JScrollPane(table),BorderLayout.CENTER); panel.add(btnPanel,BorderLayout.SOUTH);
        setContentPane(panel); pack(); setLocationRelativeTo(null);
    }

    /** Show sent table with Back button. */
    private void showSentPanel() {
        setTitle("Mailify - Sent Items");
        DefaultTableModel model = new DefaultTableModel(new Object[]{"ID","To","Subject","Date"},0);
        JTable table = new JTable(model);
        loadInboxData(model, EmailUtils.LIST_SENT, null); // reuse loader with LIST_SENT
        JButton backBtn = new JButton("Back");
        backBtn.addActionListener(e -> showMainMenuPanel());
        JPanel btnPanel = new JPanel(); btnPanel.add(backBtn);
        JPanel panel = new JPanel(new BorderLayout()); panel.add(new JScrollPane(table),BorderLayout.CENTER); panel.add(btnPanel,BorderLayout.SOUTH);
        setContentPane(panel); pack(); setLocationRelativeTo(null);
    }

    /** Helper to load inbox or sent/search results into the table model. */
    private void loadInboxData(DefaultTableModel model, String command, String term) {
        model.setRowCount(0);
        JsonObject req = new JsonObject();
        req.addProperty(EmailUtils.FIELD_COMMAND, command);
        if(term!=null) req.addProperty("term",term);
        network.send(req.toString());
        JsonObject resp = JsonParser.parseString(network.receive()).getAsJsonObject();
        String status = resp.get(EmailUtils.FIELD_STATUS).getAsString();
        if(status.equals(EmailUtils.STATUS_INBOX) || status.equals(EmailUtils.STATUS_SEARCH_RESULTS)
                || status.equals(EmailUtils.STATUS_SENT_LIST) ) {
            JsonArray arr = resp.getAsJsonArray(EmailUtils.FIELD_EMAILS);
            for(JsonElement el:arr){ JsonObject e=el.getAsJsonObject();
                model.addRow(new Object[]{
                        e.get(EmailUtils.FIELD_ID).getAsInt(),
                        command.equals(EmailUtils.LIST_SENT)
                                ? e.get(EmailUtils.FIELD_RECIPIENT).getAsString()
                                : e.get(EmailUtils.FIELD_SENDER).getAsString(),
                        e.get(EmailUtils.FIELD_SUBJECT).getAsString(),
                        e.get(EmailUtils.FIELD_TIMESTAMP).getAsString()});
            }
        }
    }

    /** Send READ command and show full content. */
    private void readEmailById(int id) {
        JsonObject req = new JsonObject();
        req.addProperty(EmailUtils.FIELD_COMMAND, EmailUtils.READ);
        req.addProperty(EmailUtils.FIELD_ID,id);
        network.send(req.toString());
        JsonObject resp = JsonParser.parseString(network.receive()).getAsJsonObject();
        String status=resp.get(EmailUtils.FIELD_STATUS).getAsString();
        if(status.equals(EmailUtils.STATUS_EMAIL_CONTENT)){
            JsonObject em=resp.getAsJsonObject(EmailUtils.FIELD_EMAIL);
            StringBuilder sb=new StringBuilder();
            sb.append("From: ").append(em.get(EmailUtils.FIELD_SENDER).getAsString()).append("\n");
            sb.append("To: ").append(em.get(EmailUtils.FIELD_RECIPIENT).getAsString()).append("\n");
            sb.append("Subject: ").append(em.get(EmailUtils.FIELD_SUBJECT).getAsString()).append("\n");
            sb.append("Date: ").append(em.get(EmailUtils.FIELD_TIMESTAMP).getAsString()).append("\n\n");
            sb.append(em.get(EmailUtils.FIELD_BODY).getAsString());
            JTextArea ta=new JTextArea(sb.toString()); ta.setEditable(false);
            JOptionPane.showMessageDialog(this,new JScrollPane(ta),"Email Content",JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /** Main entrypoint. */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new EmailClientGUI().setVisible(true));
    }
}