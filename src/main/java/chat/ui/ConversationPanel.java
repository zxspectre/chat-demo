package chat.ui;

import chat.backend.ChatService;
import chat.backend.Conversation;
import chat.backend.Message;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Panel displaying the conversation history.
 * Shows messages from the currently selected conversation, including images.
 */
public class ConversationPanel extends JPanel {
    private final ChatService chatService;

    private JLabel conversationTitle;
    private JPanel messagesPanel;
    private JScrollPane scrollPane;
    private Long currentConversationId;

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private static final int MAX_IMAGE_WIDTH = 300;
    private static final int MAX_IMAGE_HEIGHT = 200;

    public ConversationPanel(ChatService chatService) {
        this.chatService = chatService;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Conversation History",
                TitledBorder.CENTER,
                TitledBorder.TOP
        ));

        // Title showing current conversation
        conversationTitle = new JLabel("No conversation selected");
        conversationTitle.setFont(conversationTitle.getFont().deriveFont(Font.BOLD, 14f));
        conversationTitle.setHorizontalAlignment(SwingConstants.CENTER);
        conversationTitle.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(conversationTitle, BorderLayout.NORTH);

        // Messages panel with vertical layout
        messagesPanel = new JPanel();
        messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));
        messagesPanel.setBackground(Color.WHITE);

        scrollPane = new JScrollPane(messagesPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void setupMessageListener() {
        chatService.addMessageListener(message -> {
            // Update on EDT
            SwingUtilities.invokeLater(() -> {
                if (currentConversationId != null &&
                        message.getConversationId() == currentConversationId) {
                    appendMessage(message);
                    scrollToBottom();
                }
            });
        });
    }

    public void setConversation(Long conversationId) {
        this.currentConversationId = conversationId;

        if (conversationId == null) {
            conversationTitle.setText("No conversation selected");
            messagesPanel.removeAll();
            messagesPanel.revalidate();
            messagesPanel.repaint();
            return;
        }

        Conversation conversation = chatService.getConversation(conversationId);
        if (conversation != null) {
            conversationTitle.setText(conversation.getName() + " - Participants: " +
                    conversation.getParticipants());
            refreshMessages();
        }
    }

    public void refreshMessages() {
        if (currentConversationId == null) {
            return;
        }

        messagesPanel.removeAll();
        List<Message> messages = chatService.getMessages(currentConversationId);
        for (Message message : messages) {
            appendMessage(message);
        }
        messagesPanel.revalidate();
        messagesPanel.repaint();
        scrollToBottom();
    }

    private void appendMessage(Message message) {
        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
        messagePanel.setBackground(Color.WHITE);
        messagePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        messagePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Header with message ID, time and user
        String time = TIME_FORMAT.format(message.getTimestamp());
        JLabel headerLabel = new JLabel(String.format("#%d [%s] %s:", message.getId(), time, message.getSenderName()));
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 11f));
        headerLabel.setForeground(new Color(70, 70, 70));
        headerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        messagePanel.add(headerLabel);

        // Text content (if any)
        String text = message.getText();
        if (text != null && !text.isEmpty()) {
            JLabel textLabel = new JLabel("<html><body style='width: 250px'>" + escapeHtml(text) + "</body></html>");
            textLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            textLabel.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
            messagePanel.add(textLabel);
        }

        // Image (if any)
        byte[] imageData = message.getImageData();
        if (imageData != null) {
            try {
                ImageIcon originalIcon = new ImageIcon(imageData);
                Image scaledImage = scaleImage(originalIcon.getImage());
                JLabel imageLabel = new JLabel(new ImageIcon(scaledImage));
                imageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                imageLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
                messagePanel.add(imageLabel);
            } catch (Exception e) {
                JLabel errorLabel = new JLabel("[Failed to load image]");
                errorLabel.setForeground(Color.RED);
                errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                messagePanel.add(errorLabel);
            }
        }

        // Set max size to prevent horizontal expansion
        messagePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, messagePanel.getPreferredSize().height + 50));

        messagesPanel.add(messagePanel);
        messagesPanel.revalidate();
    }

    private Image scaleImage(Image original) {
        int width = original.getWidth(null);
        int height = original.getHeight(null);

        if (width <= MAX_IMAGE_WIDTH && height <= MAX_IMAGE_HEIGHT) {
            return original;
        }

        double widthRatio = (double) MAX_IMAGE_WIDTH / width;
        double heightRatio = (double) MAX_IMAGE_HEIGHT / height;
        double ratio = Math.min(widthRatio, heightRatio);

        int newWidth = (int) (width * ratio);
        int newHeight = (int) (height * ratio);

        return original.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\n", "<br>");
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = scrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    public Long getCurrentConversationId() {
        return currentConversationId;
    }
}
