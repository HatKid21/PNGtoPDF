package org.example;

import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;

public class Application extends JFrame {

    private JLabel leftIndentLabel;
    private JLabel topIndentLabel;

    private JButton muteButton;

    private String pdfName;

    private JButton removeButton;
    private JButton addButton;
    private JFileChooser fileChooser;
    private JButton saveButton;
    private JPanel previewPanel;
    private JScrollPane scrollPane;
    private JComboBox<String> languageComboBox;

    private JTextField leftIndentTextField;
    private JTextField topIndentTextField;

    private boolean isMuted = true;
    private Clip musicClip;
    private FloatControl gainControl;

    private ImageIcon imageIcon;

    private float leftIndent;
    private float topIndent;

    private static final String dir = "output";

    private PdfConverter pdfConverter;

    private Locale currentLocale;
    private ResourceBundle messages;

    private static final float DEFAULT_Y_POSITION = 1;
    private static final float DEFAULT_X_POSITION = 1.5f;

    private final List<String> imagePaths = new ArrayList<>();

    private final Preferences prefs = Preferences.userNodeForPackage(Application.class);
    private static final String PREFERRED_LANGUAGE_KEY = "preferredLanguage";

    private void loadMessages() {
        try {
            messages = ResourceBundle.getBundle("lang.messages", currentLocale);
        } catch (MissingResourceException e) {
            messages = ResourceBundle.getBundle("lang.messages", Locale.ENGLISH);
            currentLocale = Locale.ENGLISH;
        }
    }

    public void playMusic(String resourcePath) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                System.err.println("Resource not found: " + resourcePath);
                return;
            }

            // Load the entire resource into memory (byte array)
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            byte[] audioBytes = buffer.toByteArray();

            // Create a ByteArrayInputStream from the byte array
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(audioBytes);

            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(byteArrayInputStream);
            this.musicClip = AudioSystem.getClip();
            musicClip.open(audioInputStream);
            musicClip.loop(Clip.LOOP_CONTINUOUSLY);

            this.gainControl = (FloatControl) musicClip.getControl(FloatControl.Type.MASTER_GAIN);
            if (gainControl != null) {
                gainControl.setValue(gainControl.getMinimum());
            } else {
                System.err.println("Volume control not supported.");
            }
            musicClip.start();
            musicClip.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (UnsupportedAudioFileException e) {
            System.err.println("Unsupported audio format: " + e.getMessage());
            e.printStackTrace(); // Keep stack trace for detailed error info
        } catch (IOException e) {
            System.err.println("Error loading audio resource: " + e.getMessage());
            e.printStackTrace(); // Keep stack trace for detailed error info
        } catch (LineUnavailableException e) {
            System.err.println("Audio line unavailable: " + e.getMessage());
            e.printStackTrace(); // Keep stack trace for detailed error info
        }
    }

    private void enableDarkTheme(){
        try {
            UIManager.setLookAndFeel(UIManager.getLookAndFeel());
        }catch (UnsupportedLookAndFeelException e){
            System.out.println(e.getMessage());
        }

        Color darkBackground = new Color(40,40,40);
        Color lightText = Color.WHITE;
        Color buttonBackground = new Color(60,60,60);
        Color buttonForeground = lightText;

        UIManager.put("Panel.background", darkBackground);
        UIManager.put("OptionPane.background", darkBackground);
        UIManager.put("OptionPane.messageForeground", lightText);
        UIManager.put("OptionPane.foreground", lightText);
        UIManager.put("Button.background", buttonBackground);
        UIManager.put("Button.foreground", buttonForeground);
        UIManager.put("Label.foreground", lightText);
        UIManager.put("TextArea.background", darkBackground);
        UIManager.put("TextArea.foreground", lightText);
        UIManager.put("TextField.background", darkBackground);
        UIManager.put("TextField.foreground", lightText);
        UIManager.put("Viewport.background", darkBackground);
        UIManager.put("ScrollPane.background", darkBackground);
        UIManager.put("FileChooser.background", darkBackground);
        UIManager.put("ComboBox.background", darkBackground);
        UIManager.put("ComboBox.foreground", lightText);
        UIManager.put("List.background", darkBackground);
        UIManager.put("List.foreground", lightText);
        UIManager.put("MenuBar.background", darkBackground);
        UIManager.put("MenuBar.foreground", lightText);
        UIManager.put("Menu.background", darkBackground);
        UIManager.put("Menu.foreground", lightText);
        UIManager.put("MenuItem.background", darkBackground);
        UIManager.put("MenuItem.foreground", lightText);
        UIManager.put("CheckBoxMenuItem.background", darkBackground);
        UIManager.put("CheckBoxMenuItem.foreground", lightText);
        UIManager.put("RadioButtonMenuItem.background",darkBackground);
        UIManager.put("RadioButtonMenuItem.foreground", lightText);
        UIManager.put("PopupMenu.background", darkBackground);
        UIManager.put("PopupMenu.foreground", lightText);
        UIManager.put("Separator.foreground", lightText);
        UIManager.put("ToolBar.background", darkBackground);
        UIManager.put("ToolBar.foreground", lightText);
        UIManager.put("ToolTip.background", darkBackground);
        UIManager.put("ToolTip.foreground", lightText);
        UIManager.put("TitledBorder.titleColor", lightText);
        UIManager.put("FileChooser.background", darkBackground);
        UIManager.put("FileChooser.foreground", lightText);
        UIManager.put("FileChooser[Enabled].foreground", lightText);
        UIManager.put("FileChooser.listBackground", darkBackground);
        UIManager.put("FileChooser.listForeground", lightText);
        UIManager.put("FileChooser.selectionBackground", buttonBackground);
        UIManager.put("FileChooser.selectionForeground", lightText);

        SwingUtilities.updateComponentTreeUI(this);

    }

    public Application() {
        playMusic("audio/jazz.au");
        String preferredLanguage = prefs.get(PREFERRED_LANGUAGE_KEY, Locale.getDefault().getLanguage());
        currentLocale = new Locale(preferredLanguage);
        loadMessages();
        setTitle(messages.getString("app.title"));
        setSize(new Dimension(800, 800));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(0,0));
        spawnApplication();

        BufferedImage image;
        Image image1 = null;

        try (InputStream inputStream = Application.class.getClassLoader().getResourceAsStream("icon/mute.jpg")){
            if (inputStream == null){
                System.out.println("resource not found");
            }
            image = ImageIO.read(inputStream);
            image1 = image.getScaledInstance(50,50,Image.SCALE_SMOOTH);
        }catch (IOException e){
            System.out.println(e.getMessage());
        }
        imageIcon = new ImageIcon(image1);

        enableDarkTheme();
        initJThings();
        initButtonsBehaviour();
        updateUIForLanguage();

        setVisible(true);
    }

    private String getFileNameWithoutExtension(String fileName) {
        int dotIndex = fileName.indexOf(".");
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(0, dotIndex);
        }
        return fileName;
    }

    private void initButtonsBehaviour() {
        addButton.addActionListener(e -> {
            int returnVal = fileChooser.showOpenDialog(Application.this);
            if (returnVal != JFileChooser.APPROVE_OPTION) {
                JOptionPane.showMessageDialog(Application.this,
                        messages.getString("message.selectionCancel"),
                        messages.getString("message.selectionCancelTitle"),
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            File file = fileChooser.getSelectedFile();
            if (pdfConverter == null) {
                File directory = new File(dir);
                if (!directory.exists()) {
                    directory.mkdirs();
                }

                pdfName = dir + "/" + getFileNameWithoutExtension(file.getName()) + "_output.pdf";
                File pdfFile = new File(pdfName);
                if (pdfFile.exists()) {
                    int counter = 1;
                    String basePdfName = pdfName;
                    do {
                        pdfName = basePdfName.replace("_output.pdf", "_output (" + counter + ").pdf");
                        pdfFile = new File(pdfName);
                        counter++;
                    } while (pdfFile.exists());
                }

                pdfConverter = new PdfConverter(pdfName);
            }
            if (pdfConverter.addImageToQueue(file.getAbsolutePath())) {
                imagePaths.add(file.getPath());
                updateImagePreview();

            } else {
                JOptionPane.showMessageDialog(Application.this,
                        messages.getString("message.extensionError"),
                        messages.getString("message.errorTitle"),
                        JOptionPane.ERROR_MESSAGE);
            }

        });

        removeButton.addActionListener(e -> {
            if (pdfConverter != null && pdfConverter.removeImage()) {
                imagePaths.remove(imagePaths.size() - 1);
                System.out.println(messages.getString("message.removeSuccess"));
                updateImagePreview();
            } else {
                JOptionPane.showMessageDialog(Application.this,
                        messages.getString("message.nothingToRemove"),
                        messages.getString("message.errorTitle"),
                        JOptionPane.ERROR_MESSAGE);
            }

            if (pdfConverter != null && pdfConverter.getQueueLength() == 0){
                pdfConverter = null;
            }
        });

        saveButton.addActionListener(e -> {
            String leftIndentData = leftIndentTextField.getText();
            String topIndentData = topIndentTextField.getText();
            int key = proceedIndents(leftIndentData,topIndentData);
            if (imagePaths.isEmpty()){
                JOptionPane.showMessageDialog(Application.this,
                        messages.getString("message.noImagesToSave"),
                        messages.getString("message.errorTitle"),
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (key == 1){
                pdfConverter.setIndents(leftIndent, topIndent);
            } else if (key == -1){
                return;
            }
            pdfConverter.addImagesToDocument();
            pdfConverter = null;
            imagePaths.clear();
            updateImagePreview();

            openFileInExplorer();

            JOptionPane.showMessageDialog(Application.this,
                    messages.getString("message.convertSuccess"),
                    messages.getString("message.informationTitle"),
                    JOptionPane.INFORMATION_MESSAGE);
        });

        languageComboBox.addActionListener(e -> {
            String selectedLanguage = (String) languageComboBox.getSelectedItem();
            if (selectedLanguage != null) {
                switch (selectedLanguage) {
                    case "English":
                        currentLocale = Locale.ENGLISH;
                        break;
                    case "Русский":
                        currentLocale = new Locale("ru");
                        break;
                }
                prefs.put(PREFERRED_LANGUAGE_KEY, currentLocale.getLanguage());
                loadMessages();
                updateUIForLanguage();
            }
        });

        muteButton.addActionListener(e->{
            isMuted = !isMuted;
            if (isMuted){
                if (gainControl != null){
                    gainControl.setValue(gainControl.getMinimum());
                }
            }else {
                if (gainControl != null){

                    float dB = (float) (Math.log10(0.1f) * 20);

                    gainControl.setValue(dB);
                }
            }
            updateUIForLanguage();
        });


    }

    private void openFileInExplorer(){
        File file = new File(pdfName);
        if (file.exists()){
            try{
                ProcessBuilder pb = new ProcessBuilder("explorer.exe","/select,", file.getAbsolutePath());
                pb.start();
                System.out.println("File opened in explorer");
            } catch (IOException e){
                System.out.println(e.getMessage());
            }
        } else {
            System.out.println("file not exists");
        }
    }

    private int proceedIndents(String leftIndentData, String topIndentData){
        if (leftIndentData.isEmpty() && topIndentData.isEmpty()){
            return 0;
        }

        if (isStringFloat(leftIndentData)){
            leftIndent = Float.parseFloat(leftIndentData);
        } else if (!isStringFloat(leftIndentData)){
            JOptionPane.showMessageDialog(Application.this,
                    messages.getString("message.invalidNumber"),
                    messages.getString("message.errorTitle"),
                    JOptionPane.ERROR_MESSAGE);
            return -1;
        }else{
            leftIndent = DEFAULT_X_POSITION;
        }
        if (isStringFloat(topIndentData)) {
            topIndent = Float.parseFloat(topIndentData);
        } else if(!isStringFloat(leftIndentData)){
            JOptionPane.showMessageDialog(Application.this,
                    messages.getString("message.invalidNumber"),
                    messages.getString("message.errorTitle"),
                    JOptionPane.ERROR_MESSAGE);
            return -1;

        }else{
            topIndent = DEFAULT_Y_POSITION;
        }
        return 1;
    }

    private static boolean isStringFloat(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        int decimalCount = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isDigit(c)) {
                continue;
            } else if (c == '.') {
                decimalCount++;
                if (decimalCount > 1) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    private void updateUIForLanguage() {
        setTitle(messages.getString("app.title"));
        addButton.setText(messages.getString("button.add"));
        removeButton.setText(messages.getString("button.remove"));
        saveButton.setText(messages.getString("button.save"));
        topIndentLabel.setText(messages.getString("label.topIndent"));
        leftIndentLabel.setText(messages.getString("label.leftIndent"));
        muteButton.setIcon(imageIcon);

        previewPanel.removeAll();
        previewPanel.setLayout(new GridLayout());
        if (imagePaths.isEmpty()) {
            JLabel noImagesLabel = new JLabel(messages.getString("label.noImages"));
            previewPanel.add(noImagesLabel);
        } else {
            updateImagePreview();
        }
        previewPanel.revalidate();
        previewPanel.repaint();
    }

    private void initJThings() {
        this.removeButton = new JButton();
        this.addButton = new JButton();
        this.fileChooser = new JFileChooser();
        this.saveButton = new JButton();
        this.previewPanel = new JPanel();
        this.muteButton = new JButton();
        scrollPane = new JScrollPane(previewPanel);

        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE,Integer.MAX_VALUE));

        String[] languages = {"English", "Русский"};
        this.languageComboBox = new JComboBox<>(languages);
        String currentLangName;

        if (currentLocale.getLanguage().equals("ru")) {
            currentLangName = "Русский";
        } else {
            currentLangName = "English";
        }

        languageComboBox.setSelectedItem(currentLangName);
        Dimension preferredSize = languageComboBox.getPreferredSize();
        int desiredHeight = 25;

        languageComboBox.setPreferredSize(new Dimension(preferredSize.width, desiredHeight));
        languageComboBox.setMaximumSize(new Dimension(preferredSize.width, desiredHeight));

        JPanel topRightPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        topRightPanel.add(muteButton);
        topRightPanel.add(languageComboBox);

        JPanel centerPanel = new JPanel(new GridLayout(0,1));
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.PAGE_AXIS));

        this.leftIndentLabel = new JLabel(messages.getString("label.leftIndent"));
        this.topIndentLabel = new JLabel(messages.getString("label.topIndent"));

        this.leftIndentTextField = new JTextField(20);
        this.topIndentTextField = new JTextField(20);
        leftIndentTextField.setText("2");
        topIndentTextField.setText(String.valueOf(DEFAULT_Y_POSITION));

        JPanel leftIndentInput = new JPanel(new FlowLayout(FlowLayout.LEADING,5,0));
        leftIndentInput.add(leftIndentLabel);
        leftIndentInput.add(leftIndentTextField);
        leftIndentInput.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftIndentInput.setMaximumSize(new Dimension(Integer.MAX_VALUE,leftIndentInput.getPreferredSize().height));

        JPanel topIndentInput = new JPanel(new FlowLayout(FlowLayout.LEADING,5,0));
        topIndentInput.add(topIndentLabel);
        topIndentInput.add(topIndentTextField);
        topIndentInput.setAlignmentX(Component.LEFT_ALIGNMENT);
        topIndentInput.setMaximumSize(new Dimension(Integer.MAX_VALUE,topIndentInput.getPreferredSize().height));

        centerPanel.add(leftIndentInput);
        centerPanel.add(topIndentInput);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        buttonsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonsPanel.add(addButton);
        buttonsPanel.add(removeButton);
        buttonsPanel.add(saveButton);

        centerPanel.add(buttonsPanel);

        centerPanel.add(Box.createVerticalGlue());

        centerPanel.add(scrollPane);

        add(topRightPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);

        Font currentFont = UIManager.getFont("Label.font");
        Font biggerFont = currentFont.deriveFont(currentFont.getSize2D() + 4f);

        UIManager.put("Button.font", biggerFont);
        UIManager.put("Label.font", biggerFont);
        UIManager.put("TextField.font", biggerFont);
        UIManager.put("ComboBox.font", biggerFont);
        UIManager.put("List.font", biggerFont);
        UIManager.put("Menu.font", biggerFont);
        UIManager.put("MenuItem.font", biggerFont);

        SwingUtilities.updateComponentTreeUI(this);

    }

    private void updateImagePreview() {
        previewPanel.removeAll();
        previewPanel.setLayout(new GridLayout());
        if (imagePaths.isEmpty()) {
            JLabel noImagesLabel = new JLabel(messages.getString("label.noImages"));
            previewPanel.add(noImagesLabel);
            previewPanel.revalidate();
            previewPanel.repaint();
            scrollPane.getVerticalScrollBar().setValue(0);
            return;
        }
        for (String imagePath : imagePaths) {
            try {
                File imageFile = new File(imagePath);
                BufferedImage originalImage = ImageIO.read(imageFile);

                BufferedImage thumbnail = resizeImage(originalImage);

                ImageIcon imageIcon = new ImageIcon(thumbnail);
                JLabel imageLabel = new JLabel(imageIcon);

                JPanel imageContainer = new JPanel();
                imageContainer.setLayout(new BoxLayout(imageContainer, BoxLayout.Y_AXIS));
                imageContainer.setAlignmentX(Component.CENTER_ALIGNMENT);

                imageContainer.add(imageLabel);

                JLabel filenameLabel = new JLabel(imageFile.getName());
                filenameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                filenameLabel.setBorder(new EmptyBorder(0, 0, 5, 0));
                imageContainer.add(filenameLabel);

                previewPanel.add(imageContainer);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
        previewPanel.revalidate();
        previewPanel.repaint();
        scrollPane.getVerticalScrollBar().setValue(0);
    }

    private BufferedImage resizeImage(BufferedImage initialImage) {

        int maxWidth = 500;
        int maxHeight = 500;

        int originalWidth = initialImage.getWidth();
        int originalHeight = initialImage.getHeight();

        if (originalWidth <= maxWidth && originalHeight <= maxHeight) {
            return initialImage;
        }
        double widthRatio = (double) maxWidth / originalWidth;
        double heightRatio = (double) maxHeight / originalHeight;

        double scaleRatio = Math.min(widthRatio, heightRatio);

        int newWidth = (int) (originalWidth * scaleRatio);
        int newHeight = (int) (originalHeight * scaleRatio);

        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, initialImage.getType());
        Graphics2D graphics = resizedImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(initialImage, 0, 0, newWidth, newHeight, null);
        graphics.dispose();
        return resizedImage;
    }

    private void spawnApplication() {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension screenSize = toolkit.getScreenSize();
        Insets screenInsets = toolkit.getScreenInsets(getGraphicsConfiguration());
        int screenWidth = screenSize.width - screenInsets.left - screenInsets.right;
        int screenHeight = screenSize.height - screenInsets.top - screenInsets.bottom;
        int frameWidth = getWidth();
        int frameHeight = getHeight();

        int x = (screenWidth - frameWidth) / 2 + screenInsets.left;
        int y = (screenHeight - frameHeight) / 2 + screenInsets.top;

        setLocation(x, y);
    }

}
