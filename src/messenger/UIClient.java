package messenger;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.SimpleDateFormat;
import java.util.Date;

public class UIClient extends JFrame {

    private JButton actionButton;
    private JTextField textField;
    private JTextPane textPane;
    private JPanel contentPane;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("H:mm");
    private final StringBuffer content = new StringBuffer();
    private final Client client = new Client() {
        public void connected() {
            super.connected();
            UIClient.this.connected();
        }

        public void authorized() {
            super.authorized();
            UIClient.this.authorized();
        }

        public void disconnected(Exception e) {
            super.disconnected(e);
            UIClient.this.disconnected(e);
        }

        public void closedConnection(Exception e) {
            super.closedConnection(e);
            UIClient.this.closedConnection(e);
        }

        public void saidHello() {
            super.saidHello();
            UIClient.this.saidHello();
        }

        public void gotNewMessage(String nickname, String message) {
            super.gotNewMessage(nickname, message);
            UIClient.this.gotNewMessage(nickname, message);
        }
    };

    public UIClient() {
        super("Messenger");
        setContentPane(contentPane);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnect();
            }
        });
        getRootPane().setDefaultButton(actionButton);

        actionButton.addActionListener((e) -> actionButtonPressed());
        textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateButton();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateButton();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateButton();
            }
        });

        setPreferredSize(new Dimension(400, 320));
        pack();
        setLocationRelativeTo(null);
    }

    private void updateButton() {
        if (client.user != null && client.user.uid != 0) {
            String text = textField.getText();
            if (text == null || text.trim().isEmpty()) {
                actionButton.setText("Отключиться");
            } else actionButton.setText("Отправить");
        } else actionButton.setText("Подключиться");
    }

    private void actionButtonPressed() {
        String text = textField.getText();
        if (client.user == null || client.user.uid < 0) {
            requestConnect();
        } else {
            if (text == null || text.trim().isEmpty()) {
                disconnect();
            } else {
                try {
                    client.sendMessage(text);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Ошибка отправки сообщения:\n" + ex, "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
            }
            textField.setText("");
        }
    }

    private void requestConnect() {
        String address = (String) JOptionPane.showInputDialog(this, "IP сервера: ", "Подключение", JOptionPane.PLAIN_MESSAGE, null, null, "127.0.0.1");
        if (address == null || address.trim().isEmpty()) JOptionPane.showMessageDialog(this, "Введите IP адрес сервера.", "Ошибка", JOptionPane.ERROR_MESSAGE);
        else {
            try {
                client.connect(address);
                String nickname = (String) JOptionPane.showInputDialog(this, "Желаемый никнейм: ", "Авторизация", JOptionPane.PLAIN_MESSAGE, null, null, "User");
                client.authorize(nickname);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Не удалось подключиться к серверу:\n" + ex, "Ошибка", JOptionPane.ERROR_MESSAGE);
                disconnect();
            }
        }
    }

    private void connected() {
        append("Подключен к " + client.user.socket.getInetAddress().getHostAddress() + ".");
    }

    private void disconnected(Exception e) {
        append("Отключен, причина: " + e);
        updateButton();
    }

    private void authorized() {
        append("Авторизован, ID: " + client.user.uid + ", никнейм: " + client.user.nickname);
        updateButton();
    }

    private void closedConnection(Exception e) {
        append("Отключаюсь, причина: " + e);
    }

    private void saidHello() {
        append("Произвел рукопожатие.");
    }

    private void gotNewMessage(String nickname, String message) {
        append(nickname, message);
    }

    private void append(String string) {
        content.append("<font color=\"silver\">").append(string).append("</font><br>");
        update();
    }

    private void append(String nickname, String message) {
        content.append("<font color=\"blue\">").append("[").append(dateFormat.format(new Date())).append("] ").append(nickname).append(": ").append("</font>").append(message).append("<br>");
        update();
    }

    private void update() {
        textPane.setText("<html><body>" + content.toString() + "</body></html>");
    }

    private void disconnect() {
        try {
            if (client.user == null) client.disconnect();
            else client.closeConnection();
        } catch (Exception e) {
            client.disconnect(e);
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        EventQueue.invokeLater(() -> new UIClient().setVisible(true));
    }
}
