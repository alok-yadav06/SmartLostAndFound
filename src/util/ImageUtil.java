package util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * ╔══════════════════════════════════════════════════════╗
 * ║              ImageUtil — Image Handling              ║
 * ╚══════════════════════════════════════════════════════╝
 *
 * CONCEPTS COVERED:
 * - File I/O (java.nio.file.*)
 * - BufferedImage (in-memory image representation)
 * - ImageIO (read/write image files)
 * - UUID (universally unique identifier for filenames)
 * - JFileChooser (native OS file picker dialog)
 *
 * WHY UUID FOR FILENAMES?
 * If two users upload "photo.jpg", they'd overwrite each other.
 * UUID generates a unique 128-bit ID like: "3a9f4b12-7c2e-..."
 * This is how S3, Cloudinary, and all real upload systems work.
 */
public class ImageUtil {

    private static final String UPLOAD_DIR  = "resources/images/";

    static {
        // Create upload directory if it doesn't exist
        new File(UPLOAD_DIR).mkdirs();
    }

    /**
     * Opens a native file picker → copies image to our resources folder.
     * Returns the saved relative path, or null if cancelled.
     */
    public static String chooseAndSaveImage(Component parent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Item Image");
        chooser.setFileFilter(new FileNameExtensionFilter(
            "Image Files (jpg, png, gif)", "jpg", "jpeg", "png", "gif"
        ));

        int result = chooser.showOpenDialog(parent);
        if (result != JFileChooser.APPROVE_OPTION) return null;

        File selected = chooser.getSelectedFile();
        return saveImage(selected);
    }

    /**
     * Copies the selected file into our upload directory with a unique name.
     * Returns the stored path (relative to project root).
     */
    public static String saveImage(File source) {
        try {
            String ext = getExtension(source.getName());
            String uniqueName = UUID.randomUUID().toString() + "." + ext;
            Path dest = Paths.get(UPLOAD_DIR + uniqueName);

            Files.copy(source.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
            return dest.toString(); // e.g. "resources/images/abc123.jpg"

        } catch (IOException e) {
            System.err.println("Image save failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Loads an image from a path and scales it to fit a JLabel.
     * Returns a placeholder icon if path is null or file missing.
     *
     * CONCEPT: Image.SCALE_SMOOTH = bilinear interpolation (best quality)
     * SCALE_FAST = nearest-neighbor (pixelated but fast)
     */
    public static ImageIcon loadScaled(String imagePath, int width, int height) {
        if (imagePath == null || imagePath.isBlank()) {
            return createPlaceholder(width, height);
        }

        File file = new File(imagePath);
        if (!file.exists()) {
            return createPlaceholder(width, height);
        }

        try {
            BufferedImage original = ImageIO.read(file);
            Image scaled = original.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (IOException e) {
            return createPlaceholder(width, height);
        }
    }

    /**
     * Creates a grey placeholder with a camera icon.
     * Used when no image is available.
     *
     * CONCEPT: BufferedImage = image in memory (not on disk)
     * Graphics2D = 2D drawing API (lines, shapes, text, images)
     */
    public static ImageIcon createPlaceholder(int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background
        g.setColor(new Color(241, 245, 249));
        g.fillRoundRect(0, 0, width, height, 12, 12);

        // Icon placeholder
        g.setColor(new Color(148, 163, 184));
        g.setFont(new Font("Segoe UI", Font.PLAIN, 32));
        FontMetrics fm = g.getFontMetrics();
        String icon = "📷";
        int x = (width  - fm.stringWidth(icon)) / 2;
        int y = (height - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(icon, x, y);

        g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g.setColor(new Color(100, 116, 139));
        String label = "No Image";
        x = (width - g.getFontMetrics().stringWidth(label)) / 2;
        g.drawString(label, x, y + 24);

        g.dispose();
        return new ImageIcon(img);
    }

    private static String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot >= 0) ? filename.substring(dot + 1).toLowerCase() : "jpg";
    }

    /**
     * Deletes an image file from disk (e.g., when item is deleted).
     */
    public static void deleteImage(String imagePath) {
        if (imagePath == null) return;
        try {
            Files.deleteIfExists(Paths.get(imagePath));
        } catch (IOException e) {
            System.err.println("Could not delete image: " + e.getMessage());
        }
    }
}