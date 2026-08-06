package src;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Automated Production-Grade Weather Terminal.
 * Synchronizes real-time global weather data with 2D animated vector graphics
 * (Sun, Moon, Drifting Clouds, Rain, Thunderstorm, and Falling Snow).
 */
public class WeatherAppGUI extends JFrame {

    private final WeatherService weatherService;
    private final SearchHistoryManager historyManager;

    // UI Controls
    private JTextField searchField;
    private JButton searchButton;
    private JToggleButton unitToggle;
    private JComboBox<String> historyDropdown;
    private JPanel mainCard;
    private JLabel cityLabel;
    private JLabel tempLabel;
    private WeatherIconPanel mainIconPanel;
    private JLabel conditionLabel;
    private JLabel humidityLabel;
    private JLabel windLabel;
    private JLabel coordsLabel;
    private JPanel forecastPanel;
    private GradientPanel backgroundPanel;

    private WeatherData currentWeatherData;
    private boolean isMetric = true;

    // Continuous Frame Ticker for Dynamic Animations (30 FPS)
    private Timer renderAnimationTimer;
    private int animationFrame = 0;

    public WeatherAppGUI() {
        this.weatherService = new WeatherService();
        this.historyManager = new SearchHistoryManager();

        initUI();
        startGlobalAnimationLoop();
        
        // Initial automated fetch for local terminal launch
        fetchWeatherDataAsync("Nairobi");
    }

    private void initUI() {
        setTitle("Global Cyber Weather Terminal");
        setSize(920, 720);
        setMinimumSize(new Dimension(840, 640));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        backgroundPanel = new GradientPanel();
        backgroundPanel.setLayout(new BorderLayout(15, 15));
        backgroundPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // --- TOP AUTOMATED CONTROL BAR ---
        JPanel topBar = new JPanel(new BorderLayout(10, 10));
        topBar.setOpaque(false);

        JPanel searchGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchGroup.setOpaque(false);

        searchField = new JTextField("Nairobi", 14);
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        searchField.addActionListener(e -> performSearch());

        searchButton = new JButton("Search Location");
        searchButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        searchButton.setBackground(new Color(30, 41, 59));
        searchButton.setForeground(Color.WHITE);
        searchButton.setFocusPainted(false);
        searchButton.addActionListener(e -> performSearch());

        unitToggle = new JToggleButton("°C / km/h");
        unitToggle.setFont(new Font("SansSerif", Font.BOLD, 13));
        unitToggle.setBackground(new Color(51, 65, 85));
        unitToggle.setForeground(Color.WHITE);
        unitToggle.setFocusPainted(false);
        unitToggle.addActionListener(e -> toggleUnits());

        searchGroup.add(searchField);
        searchGroup.add(searchButton);
        searchGroup.add(unitToggle);

        historyDropdown = new JComboBox<>(new String[]{"Recent Searches..."});
        historyDropdown.setFont(new Font("SansSerif", Font.PLAIN, 12));
        historyDropdown.setPreferredSize(new Dimension(210, 28));
        historyDropdown.addActionListener(e -> {
            if (historyDropdown.getSelectedIndex() > 0) {
                String selected = (String) historyDropdown.getSelectedItem();
                if (selected != null && selected.contains(" (")) {
                    String query = selected.substring(0, selected.indexOf(" ("));
                    searchField.setText(query);
                    performSearch();
                }
            }
        });

        topBar.add(searchGroup, BorderLayout.WEST);
        topBar.add(historyDropdown, BorderLayout.EAST);

        // --- CENTER HERO CARD ---
        mainCard = new JPanel(new BorderLayout(15, 15));
        mainCard.setOpaque(false);

        JPanel heroPanel = new JPanel(new GridLayout(1, 2, 15, 15));
        heroPanel.setOpaque(false);

        // Left Hero: Location Metrics
        JPanel tempCard = createGlassCard();
        tempCard.setLayout(new BoxLayout(tempCard, BoxLayout.Y_AXIS));

        cityLabel = new JLabel("Loading...", SwingConstants.LEFT);
        cityLabel.setFont(new Font("SansSerif", Font.BOLD, 26));
        cityLabel.setForeground(Color.WHITE);

        coordsLabel = new JLabel("Lat: --, Lon: --", SwingConstants.LEFT);
        coordsLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        coordsLabel.setForeground(new Color(226, 232, 240));

        tempLabel = new JLabel("--°C", SwingConstants.LEFT);
        tempLabel.setFont(new Font("SansSerif", Font.BOLD, 54));
        tempLabel.setForeground(Color.WHITE);

        conditionLabel = new JLabel("Connecting...", SwingConstants.LEFT);
        conditionLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        conditionLabel.setForeground(new Color(254, 240, 138));

        tempCard.add(cityLabel);
        tempCard.add(coordsLabel);
        tempCard.add(Box.createVerticalStrut(10));
        tempCard.add(tempLabel);
        tempCard.add(conditionLabel);

        // Right Hero: Live Animated Graphic
        JPanel metricsCard = createGlassCard();
        metricsCard.setLayout(new BorderLayout(5, 5));

        mainIconPanel = new WeatherIconPanel(0, true, 130);

        JPanel textMetrics = new JPanel(new GridLayout(2, 1, 5, 5));
        textMetrics.setOpaque(false);

        humidityLabel = new JLabel("Humidity: --%", SwingConstants.CENTER);
        humidityLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        humidityLabel.setForeground(Color.WHITE);

        windLabel = new JLabel("Wind Speed: -- km/h", SwingConstants.CENTER);
        windLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        windLabel.setForeground(Color.WHITE);

        textMetrics.add(humidityLabel);
        textMetrics.add(windLabel);

        metricsCard.add(mainIconPanel, BorderLayout.CENTER);
        metricsCard.add(textMetrics, BorderLayout.SOUTH);

        heroPanel.add(tempCard);
        heroPanel.add(metricsCard);

        // --- BOTTOM FORECAST PANEL ---
        JPanel forecastContainer = new JPanel(new BorderLayout(5, 5));
        forecastContainer.setOpaque(false);

        JLabel forecastTitle = new JLabel("Short-Term 5-Day Forecast", SwingConstants.LEFT);
        forecastTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        forecastTitle.setForeground(Color.WHITE);
        forecastTitle.setBorder(new EmptyBorder(10, 5, 5, 5));

        forecastPanel = new JPanel(new GridLayout(1, 5, 10, 0));
        forecastPanel.setOpaque(false);

        forecastContainer.add(forecastTitle, BorderLayout.NORTH);
        forecastContainer.add(forecastPanel, BorderLayout.CENTER);

        mainCard.add(heroPanel, BorderLayout.CENTER);
        mainCard.add(forecastContainer, BorderLayout.SOUTH);

        backgroundPanel.add(topBar, BorderLayout.NORTH);
        backgroundPanel.add(mainCard, BorderLayout.CENTER);

        setContentPane(backgroundPanel);
    }

    private JPanel createGlassCard() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(15, 23, 42, 175));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
                g2.setColor(new Color(255, 255, 255, 40));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        return panel;
    }

    private void startGlobalAnimationLoop() {
        renderAnimationTimer = new Timer(33, e -> {
            animationFrame++;
            repaint();
        });
        renderAnimationTimer.start();
    }

    private void performSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) return;
        fetchWeatherDataAsync(query);
    }

    private void fetchWeatherDataAsync(String query) {
        searchButton.setEnabled(false);

        SwingWorker<WeatherData, Void> worker = new SwingWorker<>() {
            @Override
            protected WeatherData doInBackground() throws Exception {
                return weatherService.fetchWeather(query);
            }

            @Override
            protected void done() {
                try {
                    currentWeatherData = get();
                    historyManager.addEntry(query);
                    updateHistoryDropdown();
                    renderWeatherData();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(WeatherAppGUI.this,
                            "Could not fetch weather data: " + e.getMessage(),
                            "Network Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    searchButton.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    private void renderWeatherData() {
        if (currentWeatherData == null) return;

        cityLabel.setText(currentWeatherData.getCityName() + ", " + currentWeatherData.getCountry());
        coordsLabel.setText(String.format("Lat: %.2f, Lon: %.2f", currentWeatherData.getLatitude(), currentWeatherData.getLongitude()));
        conditionLabel.setText(currentWeatherData.getWeatherDescription());
        humidityLabel.setText("Humidity: " + currentWeatherData.getHumidity() + "%");

        mainIconPanel.setWeatherState(currentWeatherData.getWeatherCode(), currentWeatherData.isDaytime());

        updateTemperatureAndWindDisplays();
        updateDynamicBackground();
        renderForecast();
    }

    private void updateTemperatureAndWindDisplays() {
        if (currentWeatherData == null) return;

        if (isMetric) {
            tempLabel.setText(String.format("%.1f°C", currentWeatherData.getTemperatureC()));
            windLabel.setText(String.format("Wind: %.1f km/h", currentWeatherData.getWindSpeedKmH()));
        } else {
            tempLabel.setText(String.format("%.1f°F", currentWeatherData.getTemperatureF()));
            windLabel.setText(String.format("Wind: %.1f mph", currentWeatherData.getWindSpeedMph()));
        }
    }

    private void toggleUnits() {
        isMetric = !isMetric;
        unitToggle.setText(isMetric ? "°C / km/h" : "°F / mph");
        updateTemperatureAndWindDisplays();
        renderForecast();
    }

    private void updateDynamicBackground() {
        if (currentWeatherData == null) return;

        boolean isDay = currentWeatherData.isDaytime();
        int code = currentWeatherData.getWeatherCode();

        if (!isDay) {
            // Night Gradient (Deep Slate Navy)
            backgroundPanel.setGradientColors(new Color(15, 23, 42), new Color(30, 41, 59));
        } else if (code == 0 || code == 1) {
            // Clear / Sunny Sky (Cyan to Azure)
            backgroundPanel.setGradientColors(new Color(14, 116, 144), new Color(56, 189, 248));
        } else if (code >= 71 && code <= 77) {
            // Snowy Sky (Cool Ice Blue)
            backgroundPanel.setGradientColors(new Color(51, 65, 85), new Color(148, 163, 184));
        } else if (code >= 51 && code <= 99) {
            // Rainy / Stormy Sky (Dark Moody Slate)
            backgroundPanel.setGradientColors(new Color(30, 41, 59), new Color(15, 23, 42));
        } else {
            // Overcast / Cloudy (Muted Grey Sky)
            backgroundPanel.setGradientColors(new Color(71, 85, 105), new Color(30, 41, 59));
        }
    }

    private void renderForecast() {
        forecastPanel.removeAll();
        if (currentWeatherData == null || currentWeatherData.getForecastList() == null) return;

        for (WeatherData.ForecastDay day : currentWeatherData.getForecastList()) {
            JPanel card = createGlassCard();
            card.setLayout(new BorderLayout(2, 2));

            JLabel dateLbl = new JLabel(day.getDate(), SwingConstants.CENTER);
            dateLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
            dateLbl.setForeground(Color.WHITE);

            WeatherIconPanel miniIcon = new WeatherIconPanel(day.getWeatherCode(), true, 55);

            String tempStr = isMetric
                    ? String.format("%.0f° / %.0f°C", day.getMaxTempC(), day.getMinTempC())
                    : String.format("%.0f° / %.0f°F", (day.getMaxTempC() * 9 / 5) + 32, (day.getMinTempC() * 9 / 5) + 32);

            JLabel tempLbl = new JLabel(tempStr, SwingConstants.CENTER);
            tempLbl.setFont(new Font("SansSerif", Font.BOLD, 11));
            tempLbl.setForeground(new Color(226, 232, 240));

            card.add(dateLbl, BorderLayout.NORTH);
            card.add(miniIcon, BorderLayout.CENTER);
            card.add(tempLbl, BorderLayout.SOUTH);

            forecastPanel.add(card);
        }
        forecastPanel.revalidate();
        forecastPanel.repaint();
    }

    private void updateHistoryDropdown() {
        historyDropdown.removeAllItems();
        historyDropdown.addItem("Recent Searches...");
        for (SearchHistoryManager.HistoryEntry entry : historyManager.getHistory()) {
            historyDropdown.addItem(entry.toString());
        }
    }

    // --- AUTOMATED 2D VECTOR ANIMATION ENGINE ---
    private class WeatherIconPanel extends JPanel {
        private int weatherCode;
        private boolean isDaytime;
        private final int iconSize;
        private final List<Point> snowflakes = new ArrayList<>();

        public WeatherIconPanel(int weatherCode, boolean isDaytime, int iconSize) {
            this.weatherCode = weatherCode;
            this.isDaytime = isDaytime;
            this.iconSize = iconSize;
            setOpaque(false);
            initSnowflakes();
        }

        private void initSnowflakes() {
            Random rand = new Random();
            for (int i = 0; i < 15; i++) {
                snowflakes.add(new Point(rand.nextInt(60) - 30, rand.nextInt(50) - 25));
            }
        }

        public void setWeatherState(int code, boolean isDay) {
            this.weatherCode = code;
            this.isDaytime = isDay;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int cx = getWidth() / 2;
            int cy = getHeight() / 2;
            int targetDim = Math.min(getWidth(), getHeight());
            double scale = (double) targetDim / (iconSize * 1.6);
            if (scale <= 0) scale = 1.0;

            g2.translate(cx, cy);
            g2.scale(scale, scale);

            // 1. Solar / Lunar Core Graphics
            if (weatherCode == 0 || weatherCode == 1 || weatherCode == 2) {
                if (isDaytime) {
                    drawPulsingSun(g2, 0, 0, iconSize / 2);
                } else {
                    drawCrescentMoon(g2, 0, 0, iconSize / 2);
                }
            }

            // 2. Weather Condition Overlays (Rain, Snow, Clouds, Lightning)
            if (weatherCode == 0) { // Clear Sky (Subtle Floating Cyan Accent)
                double offset = Math.sin(animationFrame * 0.05) * 6;
                drawCloud(g2, (int) offset + 25, 15, (int)(iconSize * 0.45), new Color(255, 255, 255, 130));
            } else if (weatherCode == 1 || weatherCode == 2) { // Mainly Clear / Partly Cloudy
                double offset = Math.sin(animationFrame * 0.05) * 8;
                drawCloud(g2, (int) offset + 10, 10, (int)(iconSize * 0.65), new Color(241, 245, 249, 230));
            } else if (weatherCode == 3 || weatherCode == 45 || weatherCode == 48) { // Overcast / Fog
                double offset = Math.sin(animationFrame * 0.04) * 5;
                drawCloud(g2, (int) offset, 0, (int)(iconSize * 0.75), new Color(203, 213, 225, 240));
            } else if (weatherCode >= 71 && weatherCode <= 77) { // Snowing
                drawCloud(g2, 0, -8, (int)(iconSize * 0.7), new Color(226, 232, 240));
                drawAnimatedSnow(g2);
            } else if (weatherCode >= 51 && weatherCode <= 82) { // Rain / Drizzle
                drawCloud(g2, 0, -5, (int)(iconSize * 0.7), new Color(148, 163, 184));
                drawAnimatedRain(g2, 0, 20);
            } else { // Thunderstorm (95+)
                drawCloud(g2, 0, -8, (int)(iconSize * 0.7), new Color(71, 85, 105));
                drawAnimatedLightning(g2, 0, 18);
            }

            g2.dispose();
        }

        private void drawPulsingSun(Graphics2D g2, int x, int y, int radius) {
            double pulse = (Math.sin(animationFrame * 0.1) + 1.0) / 2.0;
            int rayLen = 6 + (int) (pulse * 5);

            g2.setColor(new Color(253, 224, 71, 220));
            g2.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < 8; i++) {
                double angle = Math.toRadians(i * 45 + (animationFrame * 0.5));
                int x1 = x + (int) ((radius + 3) * Math.cos(angle));
                int y1 = y + (int) ((radius + 3) * Math.sin(angle));
                int x2 = x + (int) ((radius + 3 + rayLen) * Math.cos(angle));
                int y2 = y + (int) ((radius + 3 + rayLen) * Math.sin(angle));
                g2.drawLine(x1, y1, x2, y2);
            }

            g2.setColor(new Color(234, 179, 8));
            g2.fillOval(x - radius, y - radius, radius * 2, radius * 2);
            g2.setColor(new Color(254, 240, 138));
            g2.fillOval(x - radius + 3, y - radius + 3, (radius - 3) * 2, (radius - 3) * 2);
        }

        private void drawCrescentMoon(Graphics2D g2, int x, int y, int radius) {
            g2.setColor(new Color(253, 230, 138));
            g2.fillOval(x - radius, y - radius, radius * 2, radius * 2);
            g2.setColor(new Color(15, 23, 42));
            g2.fillOval(x - radius + (radius / 2), y - radius - 2, radius * 2, radius * 2);
        }

        private void drawCloud(Graphics2D g2, int x, int y, int size, Color cloudColor) {
            g2.setColor(cloudColor);
            int w = size;
            int h = size / 2;
            g2.fillRoundRect(x - (w / 2), y, w, h, 14, 14);
            g2.fillOval(x - (w / 3), y - (h / 2), h + 4, h + 4);
            g2.fillOval(x - 2, y - (h * 3 / 4), h + 8, h + 8);
        }

        private void drawAnimatedRain(Graphics2D g2, int x, int y) {
            g2.setColor(new Color(56, 189, 248));
            g2.setStroke(new BasicStroke(2.0f));

            int fall = (animationFrame * 2) % 12;
            g2.drawLine(x - 12, y + fall, x - 15, y + 10 + fall);
            g2.drawLine(x, y + ((fall + 4) % 12), x - 3, y + 10 + ((fall + 4) % 12));
            g2.drawLine(x + 12, y + ((fall + 8) % 12), x + 9, y + 10 + ((fall + 8) % 12));
        }

        private void drawAnimatedLightning(Graphics2D g2, int x, int y) {
            if ((animationFrame / 8) % 2 == 0) {
                g2.setColor(new Color(250, 204, 21));
                Polygon bolt = new Polygon();
                bolt.addPoint(x, y - 2);
                bolt.addPoint(x - 8, y + 8);
                bolt.addPoint(x - 2, y + 8);
                bolt.addPoint(x - 6, y + 18);
                bolt.addPoint(x + 4, y + 6);
                bolt.addPoint(x, y + 6);
                g2.fillPolygon(bolt);
            }
        }

        private void drawAnimatedSnow(Graphics2D g2) {
            g2.setColor(Color.WHITE);
            for (Point pt : snowflakes) {
                int yPos = (pt.y + animationFrame) % 40 + 5;
                int xPos = pt.x + (int) (Math.sin((animationFrame + pt.x) * 0.1) * 3);
                g2.fillOval(xPos, yPos, 4, 4);
            }
        }
    }

    private static class GradientPanel extends JPanel {
        private Color colorTop = new Color(14, 116, 144);
        private Color colorBottom = new Color(56, 189, 248);

        public void setGradientColors(Color top, Color bottom) {
            this.colorTop = top;
            this.colorBottom = bottom;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            GradientPaint gp = new GradientPaint(0, 0, colorTop, 0, getHeight(), colorBottom);
            g2d.setPaint(gp);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            WeatherAppGUI gui = new WeatherAppGUI();
            gui.setVisible(true);
        });
    }
}
