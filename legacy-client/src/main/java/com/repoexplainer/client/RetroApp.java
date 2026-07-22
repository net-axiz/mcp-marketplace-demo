package com.repoexplainer.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class RetroApp extends JFrame {

    private JTextField urlField;
    private JButton analyzeButton;
    private JTextArea resultArea;
    private JTextArea debugArea;
    private JTextArea k8sArea;
    private JLabel statusLabel;
    private JCheckBox debugCheckBox;
    private JSplitPane splitPane;
    private JTabbedPane debugTabs;
    private Process kubectlProcess;
    private Process k8sEventsProcess;

    public RetroApp() {
        // Set classic Java Metal Look
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
        } catch (Exception e) {
            e.printStackTrace();
        }

        setTitle("Repo Explainer - Classic Edition");
        setSize(900, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Top Panel: URL Input, Checkbox and Button
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout(10, 10));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        topPanel.setBackground(Color.LIGHT_GRAY);

        JLabel promptLabel = new JLabel("GitHub Repo URL: ");
        promptLabel.setFont(new Font("Tahoma", Font.BOLD, 12));
        urlField = new JTextField("https://github.com/OpenCut-app/OpenCut");
        urlField.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        JPanel rightTopPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightTopPanel.setBackground(Color.LIGHT_GRAY);
        
        debugCheckBox = new JCheckBox("Debug Mode");
        debugCheckBox.setFont(new Font("Tahoma", Font.PLAIN, 12));
        debugCheckBox.setBackground(Color.LIGHT_GRAY);
        debugCheckBox.addActionListener(e -> toggleDebugView());

        analyzeButton = new JButton("Analyze");
        analyzeButton.setFont(new Font("Tahoma", Font.BOLD, 12));
        analyzeButton.setBackground(new Color(220, 220, 220));

        rightTopPanel.add(debugCheckBox);
        rightTopPanel.add(analyzeButton);

        topPanel.add(promptLabel, BorderLayout.WEST);
        topPanel.add(urlField, BorderLayout.CENTER);
        topPanel.add(rightTopPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // Result Area
        resultArea = new JTextArea();
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultArea.setEditable(false);
        resultArea.setBackground(new Color(250, 250, 250));
        JScrollPane resultScrollPane = new JScrollPane(resultArea);
        resultScrollPane.setBorder(BorderFactory.createTitledBorder("Analysis Result"));

        // Debug Area (Spring Boot Logs)
        debugArea = new JTextArea();
        debugArea.setFont(new Font("Monospaced", Font.BOLD, 12));
        debugArea.setForeground(Color.GREEN);
        debugArea.setBackground(Color.BLACK);
        debugArea.setEditable(false);
        JScrollPane debugScrollPane = new JScrollPane(debugArea);

        // K8s Events Area
        k8sArea = new JTextArea();
        k8sArea.setFont(new Font("Monospaced", Font.BOLD, 12));
        k8sArea.setForeground(Color.CYAN);
        k8sArea.setBackground(Color.BLACK);
        k8sArea.setEditable(false);
        JScrollPane k8sScrollPane = new JScrollPane(k8sArea);

        // Tabbed Pane
        debugTabs = new JTabbedPane();
        debugTabs.addTab("Spring Boot Logs", debugScrollPane);
        debugTabs.addTab("Kubernetes Events", k8sScrollPane);

        // Split Pane
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, resultScrollPane, debugTabs);
        splitPane.setResizeWeight(0.6);
        add(splitPane, BorderLayout.CENTER);

        // Bottom Panel
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel.setBorder(BorderFactory.createEtchedBorder());
        statusLabel = new JLabel("Ready.");
        statusLabel.setFont(new Font("Tahoma", Font.PLAIN, 11));
        bottomPanel.add(statusLabel);
        add(bottomPanel, BorderLayout.SOUTH);

        // Initial state
        toggleDebugView();

        // Event Listener
        analyzeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String url = urlField.getText().trim();
                if (url.isEmpty()) {
                    JOptionPane.showMessageDialog(RetroApp.this, "Please enter a GitHub URL.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                analyzeButton.setEnabled(false);
                resultArea.setText("");
                debugArea.setText("");
                k8sArea.setText("");
                statusLabel.setText("Status: Connecting to server, running LLM (may take 1-3 minutes)...");

                if (debugCheckBox.isSelected()) {
                    startLogStream();
                }

                fetchExplanationAsync(url);
            }
        });
    }

    private void toggleDebugView() {
        if (debugCheckBox.isSelected()) {
            splitPane.setBottomComponent(debugTabs);
            splitPane.setDividerLocation(0.6);
            startLogStream();
        } else {
            splitPane.setBottomComponent(null);
            stopLogStream();
        }
    }

    private void appendLog(JTextArea area, String message) {
        SwingUtilities.invokeLater(() -> {
            area.append(message + "\n");
            area.setCaretPosition(area.getDocument().getLength());
        });
    }

    private void startLogStream() {
        stopLogStream();
        appendLog(debugArea, "[RetroApp] Starting Spring Boot log stream...");
        appendLog(k8sArea, "[RetroApp] Starting Kubernetes event stream...");
        
        // Spring Boot Logs
        CompletableFuture.runAsync(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(
                        "kubectl", "logs", "-n", "repo-explainer", "deployment/repo-explainer", "-f", "--tail=20"
                );
                pb.redirectErrorStream(true);
                kubectlProcess = pb.start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(kubectlProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        appendLog(debugArea, line);
                    }
                }
            } catch (Exception e) {
                appendLog(debugArea, "[ERROR] Failed to read logs: " + e.getMessage());
            }
        });

        // Kubernetes Events
        CompletableFuture.runAsync(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(
                        "kubectl", "get", "events", "-n", "repo-explainer", "--watch"
                );
                pb.redirectErrorStream(true);
                k8sEventsProcess = pb.start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(k8sEventsProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        appendLog(k8sArea, line);
                    }
                }
            } catch (Exception e) {
                appendLog(k8sArea, "[ERROR] Failed to read events: " + e.getMessage());
            }
        });
    }

    private void stopLogStream() {
        if (kubectlProcess != null && kubectlProcess.isAlive()) {
            kubectlProcess.destroy();
            kubectlProcess = null;
            appendLog(debugArea, "[RetroApp] Log stream stopped.");
        }
        if (k8sEventsProcess != null && k8sEventsProcess.isAlive()) {
            k8sEventsProcess.destroy();
            k8sEventsProcess = null;
            appendLog(k8sArea, "[RetroApp] Event stream stopped.");
        }
    }

    private void fetchExplanationAsync(String repoUrl) {
        CompletableFuture.runAsync(() -> {
            try {
                appendLog(debugArea, "[RetroApp] Preparing HTTP request: http://localhost:8084/api/legacy-explain?url=" + repoUrl);
                
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build();

                String apiUrl = "http://localhost:8084/api/legacy-explain?url=" + repoUrl;

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl))
                        .timeout(Duration.ofMinutes(10))
                        .GET()
                        .build();

                appendLog(debugArea, "[RetroApp] Request sent, waiting for LLM response (this may take a while)...");
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                appendLog(debugArea, "[RetroApp] Response received! Status code: " + response.statusCode());

                SwingUtilities.invokeLater(() -> {
                    if (response.statusCode() == 200) {
                        resultArea.setText(response.body());
                        statusLabel.setText("Status: Analysis completed successfully.");
                    } else {
                        resultArea.setText("Server Error: " + response.statusCode() + "\n\n" + response.body());
                        statusLabel.setText("Status: Error occurred.");
                    }
                    analyzeButton.setEnabled(true);
                    stopLogStream();
                });

            } catch (Exception ex) {
                appendLog(debugArea, "[ERROR] HTTP request failed: " + ex.getMessage());
                SwingUtilities.invokeLater(() -> {
                    resultArea.setText("Connection Error: " + ex.getMessage() + "\n\n(Note: Make sure kubectl port-forward is running)");
                    statusLabel.setText("Status: Connection failed.");
                    analyzeButton.setEnabled(true);
                    stopLogStream();
                });
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            RetroApp app = new RetroApp();
            app.setVisible(true);
        });
    }
}
