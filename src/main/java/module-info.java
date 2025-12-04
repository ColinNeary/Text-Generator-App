module com.example.javafxproject {
    // JavaFX modules
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base;

    // Database (JDBC)
    requires java.sql; // ✅ Needed for MySQL database connection

    // Third-party libraries
    requires org.apache.pdfbox;
    requires org.apache.poi.ooxml;

    // Optional (used internally by Apache POI)
    requires org.apache.commons.io;
    requires org.apache.xmlbeans;
    requires java.xml;

    // To use FileChooser and other FX UI elements
    requires java.desktop;

    // Open your main package for FXML and reflection
    opens com.example.javafxproject to javafx.fxml;

    // Export the same package so other modules can access it
    exports com.example.javafxproject;
}