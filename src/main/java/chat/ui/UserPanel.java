package chat.ui;

import chat.backend.ChatService;
import chat.backend.UserProfile;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Panel representing a user's view of the chat.
 * Allows entering user name and sending messages to the current conversation.
 */
public class UserPanel extends JPanel {
    private final ChatService chatService;
    private final String panelTitle;

    private JTextField userNameField;
    private JTextField ageField;
    private JComboBox<String> eyeColorCombo;
    private JTextArea messageInput;
    private JButton sendButton;
    private JButton attachImageButton;
    private byte[] pendingImage;
    private JLabel imagePreviewLabel;

    private Long currentConversationId;
    private Consumer<String> nameChangeListener;

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

        // Top panel: user profile metadata (name, age, eye color)
        JPanel profilePanel = new JPanel();
        profilePanel.setLayout(new BoxLayout(profilePanel, BoxLayout.Y_AXIS));
        profilePanel.setBorder(BorderFactory.createTitledBorder("Profile"));

        userNameField = new JTextField(10);
        userNameField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { fireNameChanged(); }
            @Override
            public void removeUpdate(DocumentEvent e) { fireNameChanged(); }
            @Override
            public void changedUpdate(DocumentEvent e) { fireNameChanged(); }
        });
        profilePanel.add(makeFieldRow("Name:", userNameField));

        ageField = new JTextField(4);
        ageField.setText("18");
        profilePanel.add(makeFieldRow("Age:", ageField));

        eyeColorCombo = new JComboBox<>(new String[]{
                "", "Brown", "Blue", "Green", "Hazel", "Gray", "Amber", "Black"});
        eyeColorCombo.setEditable(true);
        eyeColorCombo.setSelectedItem("Brown");
        profilePanel.add(makeFieldRow("Eye color:", eyeColorCombo));

        add(profilePanel, BorderLayout.NORTH);

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
        makeButtonBig(attachImageButton);
        buttonPanel.add(attachImageButton);

        sendButton = new JButton("Send");
        sendButton.addActionListener(e -> sendMessage());
        makeButtonBig(sendButton);
        buttonPanel.add(sendButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * Enlarges a button's font and preferred size to make it more prominent.
     */
    private void makeButtonBig(JButton button) {
        button.setFont(button.getFont().deriveFont(Font.BOLD, 16f));
        button.setPreferredSize(new Dimension(160, 45));
    }

    /**
     * Builds a left-aligned "Label: [field]" row with a fixed-width label so the
     * profile fields line up vertically.
     */
    private JPanel makeFieldRow(String labelText, JComponent field) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        JLabel label = new JLabel(labelText);
        label.setPreferredSize(new Dimension(70, 25));
        row.add(label);
        row.add(field);
        return row;
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

        // Parse optional age metadata.
        Integer age = null;
        String ageText = ageField.getText().trim();
        if (!ageText.isEmpty()) {
            try {
                age = Integer.parseInt(ageText);
                if (age < 0 || age > 150) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,
                        "Age must be a whole number between 0 and 150.",
                        "Invalid Age",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        // Optional eye color metadata (editable combo box).
        Object selectedColor = eyeColorCombo.getSelectedItem();
        String eyeColor = selectedColor == null ? null : selectedColor.toString().trim();
        if (eyeColor != null && eyeColor.isEmpty()) {
            eyeColor = null;
        }

        chatService.registerUserProfile(new UserProfile(userName, age, eyeColor));
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

    public void setAge(Integer age) {
        ageField.setText(age == null ? "" : String.valueOf(age));
    }

    public void setEyeColor(String eyeColor) {
        eyeColorCombo.setSelectedItem(eyeColor == null ? "" : eyeColor);
    }

    /**
     * Populates the editable fields from a stored profile, leaving the
     * constructor defaults untouched when no profile exists.
     */
    public void loadProfile(UserProfile profile) {
        if (profile == null) {
            return;
        }
        setAge(profile.getAge());
        setEyeColor(profile.getEyeColor());
    }

    /**
     * Registers a listener that is notified whenever the participant's name changes.
     * Used by the parent container to keep tab titles in sync.
     */
    public void setNameChangeListener(Consumer<String> listener) {
        this.nameChangeListener = listener;
    }

    private void fireNameChanged() {
        if (nameChangeListener != null) {
            nameChangeListener.accept(getUserName());
        }
    }
}
