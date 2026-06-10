package chat.ui;

import chat.backend.ChatService;
import chat.backend.Conversation;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Main application class for the Chat Demo.
 * Creates the main window with:
 * - Left: Conversation history display
 * - Right: A group of participants (one by default), each in its own tab,
 *   with controls to add or remove participants.
 */
public class ChatApplication extends JFrame {
    private final ChatService chatService;

    private ConversationPanel conversationPanel;
    private JTabbedPane participantTabs;
    private final List<UserPanel> userPanels = new ArrayList<>();
    private JComboBox<ConversationItem> conversationCombo;

    private int participantCounter = 0;
    private Long currentConversationId;

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
        conversationPanel = new ConversationPanel(chatService);

        // Participants area: tabs of user panels plus add/remove controls
        JPanel participantsContainer = new JPanel(new BorderLayout(5, 5));

        JPanel participantControls = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton addParticipantButton = new JButton("Add Participant");
        addParticipantButton.addActionListener(e -> onAddParticipantClicked());
        makeButtonBig(addParticipantButton);
        participantControls.add(addParticipantButton);

        JButton removeParticipantButton = new JButton("Remove Participant");
        removeParticipantButton.addActionListener(e -> removeSelectedParticipant());
        makeButtonBig(removeParticipantButton);
        participantControls.add(removeParticipantButton);

        participantTabs = new JTabbedPane();

        participantsContainer.add(participantControls, BorderLayout.NORTH);
        participantsContainer.add(participantTabs, BorderLayout.CENTER);

        // Create control panel at top
        JPanel controlPanel = createControlPanel();

        // Layout
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));

        // Use JSplitPane for resizable panels: conversation on the left,
        // participants on the right.
        JSplitPane mainSplit = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                conversationPanel,
                participantsContainer
        );
        mainSplit.setResizeWeight(0.6);

        mainPanel.add(controlPanel, BorderLayout.NORTH);
        mainPanel.add(mainSplit, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);

        // Participant tabs are populated from the selected conversation in
        // onConversationSelected() (called from the constructor).
    }

    /**
     * Handles an explicit "Add Participant" click: creates a new participant tab
     * and registers it with the current conversation so it persists when the
     * user switches conversations and back.
     */
    private void onAddParticipantClicked() {
        UserPanel panel = addParticipant(null);
        if (currentConversationId != null) {
            chatService.addParticipant(currentConversationId, panel.getUserName());
            // Refresh the conversation header to include the new participant.
            conversationPanel.setConversation(currentConversationId);
        }
    }

    /**
     * Rebuilds the participant tabs to match the given conversation's
     * participants. Falls back to a single default participant when the
     * conversation has none.
     */
    private void rebuildParticipantTabs(Conversation conversation) {
        participantTabs.removeAll();
        userPanels.clear();

        List<String> names = new ArrayList<>(conversation.getParticipants());
        if (names.isEmpty()) {
            addParticipant(null);
        } else {
            for (String name : names) {
                addParticipant(name);
            }
        }
    }

    /**
     * Enlarges a button's font and preferred size to make it more prominent.
     */
    private void makeButtonBig(JButton button) {
        button.setFont(button.getFont().deriveFont(Font.BOLD, 16f));
        button.setPreferredSize(new Dimension(200, 45));
    }

    /**
     * Adds a new participant panel as a tab. If {@code name} is null or blank, a
     * default name ("User N") is generated. Any stored profile metadata for the
     * name is loaded into the panel.
     *
     * @return the newly created panel
     */
    private UserPanel addParticipant(String name) {
        String participantName;
        if (name != null && !name.trim().isEmpty()) {
            participantName = name.trim();
        } else {
            participantCounter++;
            participantName = "User " + participantCounter;
        }

        UserPanel panel = new UserPanel(participantName, chatService);
        panel.setUserName(participantName);
        panel.loadProfile(chatService.getUserProfile(participantName));
        panel.setCurrentConversation(currentConversationId);

        userPanels.add(panel);
        participantTabs.addTab(participantName, panel);
        int index = participantTabs.indexOfComponent(panel);

        // Keep the tab title in sync with the editable name field.
        panel.setNameChangeListener(newName -> {
            int idx = participantTabs.indexOfComponent(panel);
            if (idx >= 0) {
                participantTabs.setTitleAt(idx, newName.isEmpty() ? "(unnamed)" : newName);
            }
        });

        participantTabs.setSelectedIndex(index);
        return panel;
    }

    private void removeSelectedParticipant() {
        if (userPanels.size() <= 1) {
            JOptionPane.showMessageDialog(this,
                    "A group chat needs at least one participant.",
                    "Cannot Remove",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int index = participantTabs.getSelectedIndex();
        if (index < 0) {
            return;
        }

        Component component = participantTabs.getComponentAt(index);
        if (component instanceof UserPanel && currentConversationId != null) {
            // Remove from the conversation so it doesn't reappear on switch.
            chatService.removeParticipant(currentConversationId, ((UserPanel) component).getUserName());
            conversationPanel.setConversation(currentConversationId);
        }
        participantTabs.removeTabAt(index);
        userPanels.remove(component);
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
            currentConversationId = item.conversation.getId();
            conversationPanel.setConversation(currentConversationId);
            // Rebuild participant tabs to match the selected conversation.
            rebuildParticipantTabs(item.conversation);
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
