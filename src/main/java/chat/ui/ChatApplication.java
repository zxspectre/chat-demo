package chat.ui;

import chat.backend.ChatService;
import chat.backend.Conversation;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Main application class for the Chat Demo.
 * Creates the main window with three panels:
 * - Left: User panel for one user
 * - Center: Conversation history display
 * - Right: User panel for another user
 */
public class ChatApplication extends JFrame {
    private final ChatService chatService;

    private UserPanel leftUserPanel;
    private ConversationPanel conversationPanel;
    private UserPanel rightUserPanel;
    private JComboBox<ConversationItem> conversationCombo;

    public ChatApplication() {
        super("Chat Demo");
        this.chatService = new ChatService();

        initComponents();
        createSampleData();
        conversationCombo.addActionListener(e -> onConversationSelected());
        conversationPanel.setupMessageListener();
        onConversationSelected();
        pack();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(900, 500));
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane()).setBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create panels
        leftUserPanel = new UserPanel("Left User", chatService);
        conversationPanel = new ConversationPanel(chatService);
        rightUserPanel = new UserPanel("Right User", chatService);

        // Set different default user names
        leftUserPanel.setUserName("Alice");
        rightUserPanel.setUserName("Bob");

        // Create control panel at top
        JPanel controlPanel = createControlPanel();

        // Layout
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));

        // Use JSplitPane for resizable panels
        JSplitPane rightSplit = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                conversationPanel,
                rightUserPanel
        );
        rightSplit.setResizeWeight(0.7);

        JSplitPane mainSplit = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                leftUserPanel,
                rightSplit
        );
        mainSplit.setResizeWeight(0.15);

        mainPanel.add(controlPanel, BorderLayout.NORTH);
        mainPanel.add(mainSplit, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Conversation Management"));

        // Conversation selector
        panel.add(new JLabel("Conversation:"));
        conversationCombo = new JComboBox<>();
        conversationCombo.setPreferredSize(new Dimension(200, 25));
        panel.add(conversationCombo);

        // Create new conversation button
        JButton createButton = new JButton("New Conversation");
        createButton.addActionListener(e -> createNewConversation());
        panel.add(createButton);

        return panel;
    }

    private void onConversationSelected() {
        ConversationItem item = (ConversationItem) conversationCombo.getSelectedItem();
        if (item != null) {
            Long conversationId = item.conversation.getId();
            // Update all panels with the selected conversation
            leftUserPanel.setCurrentConversation(conversationId);
            rightUserPanel.setCurrentConversation(conversationId);
            conversationPanel.setConversation(conversationId);
        }
    }

    private void refreshConversationCombo() {
        Long selectedId = null;
        ConversationItem selected = (ConversationItem) conversationCombo.getSelectedItem();
        if (selected != null) {
            selectedId = selected.conversation.getId();
        }

        conversationCombo.removeAllItems();
        List<Conversation> conversations = chatService.getAllConversations();
        for (Conversation conv : conversations) {
            conversationCombo.addItem(new ConversationItem(conv));
        }

        // Restore selection
        if (selectedId != null) {
            for (int i = 0; i < conversationCombo.getItemCount(); i++) {
                if (conversationCombo.getItemAt(i).conversation.getId() == selectedId) {
                    conversationCombo.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private void createNewConversation() {
        String name = JOptionPane.showInputDialog(this,
                "Enter conversation name:",
                "New Conversation",
                JOptionPane.QUESTION_MESSAGE);

        if (name != null && !name.trim().isEmpty()) {
            String participantsStr = JOptionPane.showInputDialog(this,
                    "Enter participant names (comma-separated, e.g., Alice, Bob, Charlie):",
                    "Add Participants",
                    JOptionPane.QUESTION_MESSAGE);

            java.util.List<String> participants = new java.util.ArrayList<>();
            if (participantsStr != null && !participantsStr.trim().isEmpty()) {
                String[] parts = participantsStr.split(",");
                for (String part : parts) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        participants.add(trimmed);
                    }
                }
            }

            Conversation conv = chatService.createConversation(name.trim(), participants);
            refreshConversationCombo();

            // Select the newly created conversation
            for (int i = 0; i < conversationCombo.getItemCount(); i++) {
                if (conversationCombo.getItemAt(i).conversation.getId() == conv.getId()) {
                    conversationCombo.setSelectedIndex(i);
                    break;
                }
            }

            JOptionPane.showMessageDialog(this,
                    "Conversation '" + conv.getName() + "' created.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void createSampleData() {
        // Create some sample conversations
        chatService.createConversation("General Chat", Arrays.asList("Alice", "Bob", "Charlie"));
        chatService.createConversation("Random", Collections.emptyList());

        // Add some sample messages
        chatService.sendMessage(1L, "Alice", "Hello everyone!");
        chatService.sendMessage(1L, "Charlie", "Hi all!");


        // Refresh and select first conversation
        refreshConversationCombo();
    }

    public static void main(String[] args) {
        // Set look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Use default look and feel
        }

        // Run on EDT
        SwingUtilities.invokeLater(() -> {
            ChatApplication app = new ChatApplication();
            app.setVisible(true);
        });
    }

    /**
     * Wrapper class for displaying conversations in ComboBox.
     */
    private static class ConversationItem {
        final Conversation conversation;

        ConversationItem(Conversation conversation) {
            this.conversation = conversation;
        }

        @Override
        public String toString() {
            return conversation.getName() + " (#" + conversation.getId() + ")";
        }
    }
}
