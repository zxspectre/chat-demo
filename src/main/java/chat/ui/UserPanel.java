package chat.ui;

import chat.backend.ChatService;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Panel representing a user's view of the chat.
 * Allows entering user name and sending messages to the current conversation.
 */
public class UserPanel extends JPanel {
    private final ChatService chatService;
    private final String panelTitle;

    private JTextField userNameField;
    private JTextArea messageInput;
    private JButton sendButton;
    private JButton attachImageButton;
    private byte[] pendingImage;
    private JLabel imagePreviewLabel;

    private Long currentConversationId;

    public UserPanel(String title, ChatService chatService) {
        this.panelTitle = title;
        this.chatService = chatService;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                panelTitle,
                TitledBorder.CENTER,
                TitledBorder.TOP
        ));

        // Top panel: User name input
        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        userPanel.add(new JLabel("Name:"));
        userNameField = new JTextField(10);
        userNameField.setPreferredSize(new Dimension(100, 25));
        userPanel.add(userNameField);

        add(userPanel, BorderLayout.NORTH);

        // Center: Message input with image preview
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));

        messageInput = new JTextArea(3, 20);
        messageInput.setLineWrap(true);
        messageInput.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(messageInput);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Message"));
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        // Image preview area
        imagePreviewLabel = new JLabel("No image attached");
        imagePreviewLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imagePreviewLabel.setPreferredSize(new Dimension(100, 60));
        imagePreviewLabel.setBorder(BorderFactory.createTitledBorder("Image"));
        centerPanel.add(imagePreviewLabel, BorderLayout.SOUTH);

        add(centerPanel, BorderLayout.CENTER);

        // Bottom: Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));

        attachImageButton = new JButton("Attach Image");
        attachImageButton.addActionListener(e -> attachImage());
        buttonPanel.add(attachImageButton);

        sendButton = new JButton("Send");
        sendButton.addActionListener(e -> sendMessage());
        buttonPanel.add(sendButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void attachImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(java.io.File f) {
                if (f.isDirectory()) return true;
                String name = f.getName().toLowerCase();
                return name.endsWith(".png") || name.endsWith(".jpg") ||
                       name.endsWith(".jpeg") || name.endsWith(".gif");
            }
            @Override
            public String getDescription() {
                return "Image files (*.png, *.jpg, *.jpeg, *.gif)";
            }
        });

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                java.io.File file = fileChooser.getSelectedFile();
                pendingImage = java.nio.file.Files.readAllBytes(file.toPath());

                // Show thumbnail preview
                ImageIcon icon = new ImageIcon(pendingImage);
                Image scaled = icon.getImage().getScaledInstance(80, 50, Image.SCALE_SMOOTH);
                imagePreviewLabel.setIcon(new ImageIcon(scaled));
                imagePreviewLabel.setText("");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "Failed to load image: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
                pendingImage = null;
            }
        }
    }

    private void clearImagePreview() {
        pendingImage = null;
        imagePreviewLabel.setIcon(null);
        imagePreviewLabel.setText("No image attached");
    }

    private void sendMessage() {
        if (currentConversationId == null) {
            JOptionPane.showMessageDialog(this,
                    "Please select a conversation first.",
                    "No Conversation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String userName = userNameField.getText().trim();
        if (userName.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter your name.",
                    "No Name",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String text = messageInput.getText().trim();
        if (text.isEmpty() && pendingImage == null) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a message or attach an image.",
                    "Empty Message",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        chatService.sendMessage(currentConversationId, userName, text, pendingImage);
        messageInput.setText("");
        clearImagePreview();
    }

    public void setCurrentConversation(Long conversationId) {
        this.currentConversationId = conversationId;
    }

    public String getUserName() {
        return userNameField.getText().trim();
    }

    public void setUserName(String userName) {
        userNameField.setText(userName);
    }
}
