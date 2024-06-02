package com.mycompany.acsi2;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;

public class ACSI2 extends JFrame {
    private static final String LOGIN_URL = "http://localhost:3000/Users/login";
    private static final String SHOW_PRODUCTS_URL = "http://localhost:3000/Nomenclatures/showProducts";
    static final String ADD_PRODUCT_URL = "http://localhost:3000/Nomenclatures/addProduct";

    private JPanel cardPanel;
    private CardLayout cardLayout;

    public ACSI2() {
        setTitle("ACSI2");
        setSize(800, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        LoginPanel loginPanel = new LoginPanel();
        cardPanel.add(loginPanel, "login");

        ProductsPanel productsPanel = new ProductsPanel();
        cardPanel.add(productsPanel, "products");

        add(cardPanel);

        loginPanel.addLoginListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String email = loginPanel.getEmail();
                String password = loginPanel.getPassword();
                boolean loginSuccessful = performLogin(email, password);
                if (loginSuccessful) {
                    cardLayout.show(cardPanel, "products");
                    try {
                        String[][] products = getProducts();
                        productsPanel.setProducts(products);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else {
                    JOptionPane.showMessageDialog(ACSI2.this, "Login failed", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    private boolean performLogin(String email, String password) {
        try {
            URL url = new URI(LOGIN_URL).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String jsonInputString = "{\"email\": \"" + email + "\", \"password\": \"" + password + "\"}";

            conn.getOutputStream().write(jsonInputString.getBytes());
            if (conn.getResponseCode() == 200) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private String[][] getProducts() throws Exception {
        URL url = new URI(SHOW_PRODUCTS_URL).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        conn.disconnect();
        // Parse JSON response and return products data as a 2D array
        return parseProducts(response.toString());
    }

    private String[][] parseProducts(String json) {
        ArrayList<String[]> productList = new ArrayList<>();
        JSONObject jsonObject = new JSONObject(json);
        if (jsonObject.has("response") && jsonObject.get("response") instanceof JSONArray) {
            JSONArray jsonArray = jsonObject.getJSONArray("response");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject productObject = jsonArray.getJSONObject(i);
                String designation = productObject.getString("designation");
                int quantite = productObject.getInt("quantite");
                int seuil = productObject.getInt("seuil");
                productList.add(new String[]{designation, Integer.toString(quantite), Integer.toString(seuil)});
            }
        }
        String[][] productsArray = new String[productList.size()][];
        return productList.toArray(productsArray);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                ACSI2 app = new ACSI2();
                app.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}

class LoginPanel extends JPanel {
    private JTextField emailField;
    private JPasswordField passwordField;
    private JButton loginButton;

    public LoginPanel() {
        setLayout(new GridLayout(3, 2));
        add(new JLabel("Email:"));
        emailField = new JTextField();
        add(emailField);
        add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        add(passwordField);
        loginButton = new JButton("Login");
        add(loginButton);
    }

    public String getEmail() {
        return emailField.getText();
    }

    public String getPassword() {
        return new String(passwordField.getPassword());
    }

    public void addLoginListener(ActionListener listener) {
        loginButton.addActionListener(listener);
    }
}

class ProductsPanel extends JPanel {
    private JTable table;
    private JButton refreshButton;
    private JButton addButton;
    private AddProductPanel addProductPanel;

    public ProductsPanel() {
        setLayout(new BorderLayout());
        table = new JTable();
        add(new JScrollPane(table), BorderLayout.CENTER);

        refreshButton = new JButton("Afficher Produits");
        add(refreshButton, BorderLayout.NORTH);

        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String[][] products = getProducts();
                    setProducts(products);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(ProductsPanel.this, "Erreur lors de la récupération des produits", "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        addButton = new JButton("Ajouter Produit");
        add(addButton, BorderLayout.SOUTH);

        addProductPanel = new AddProductPanel();

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int option = JOptionPane.showConfirmDialog(ProductsPanel.this, addProductPanel, "Ajouter Produit", JOptionPane.OK_CANCEL_OPTION);
                if (option == JOptionPane.OK_OPTION) {
                    String designation = addProductPanel.getDesignation();
                    int quantity = addProductPanel.getQuantity();
                    int threshold = addProductPanel.getThreshold();
                    try {
                        addProduct(designation, quantity, threshold);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(ProductsPanel.this, "Erreur lors de l'ajout du produit", "Erreur", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
    }

    private void addProduct(String designation, int quantity, int threshold) throws Exception {
        // Appel API pour ajouter un produit
        URL url = new URI(ACSI2.ADD_PRODUCT_URL).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        JSONObject jsonInput = new JSONObject();
        jsonInput.put("designation", designation);
        jsonInput.put("quantite", quantity);
        jsonInput.put("seuil", threshold);

        conn.getOutputStream().write(jsonInput.toString().getBytes());

        if (conn.getResponseCode() == 200) {
            // Rafraîchir la liste des produits
            String[][] products = getProducts();
            setProducts(products);
        } else {
            throw new Exception("Erreur lors de l'ajout du produit : " + conn.getResponseCode());
        }
    }

    public void setProducts(String[][] productsData) {
        String[] columnNames = {"Désignation", "Quantité", "Seuil"};
        DefaultTableModel tableModel = new DefaultTableModel(productsData, columnNames);
        table.setModel(tableModel);
    }

    private String[][] getProducts() throws Exception {
        URL url = new URI("http://localhost:3000/Nomenclatures/showProducts").toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        conn.disconnect();
        // Parse JSON response and return products data as a 2D array
        return parseProducts(response.toString());
    }

    private String[][] parseProducts(String json) {
        ArrayList<String[]> productList = new ArrayList<>();
        JSONObject jsonObject = new JSONObject(json);
        if (jsonObject.has("response") && jsonObject.get("response") instanceof JSONArray) {
            JSONArray jsonArray = jsonObject.getJSONArray("response");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject productObject = jsonArray.getJSONObject(i);
                String designation = productObject.getString("designation");
                int quantite = productObject.getInt("quantite");
                int seuil = productObject.getInt("seuil");
                productList.add(new String[]{designation, Integer.toString(quantite), Integer.toString(seuil)});
            }
        }
        String[][] productsArray = new String[productList.size()][];
        return productList.toArray(productsArray);
    }
}

class AddProductPanel extends JPanel {
    private JTextField designationField;
    private JTextField quantityField;
    private JTextField thresholdField;
    private JButton addButton;

    public AddProductPanel() {
        setLayout(new GridLayout(4, 2));
        add(new JLabel("Désignation:"));
        designationField = new JTextField();
        add(designationField);
        add(new JLabel("Quantité:"));
        quantityField = new JTextField();
        add(quantityField);
        add(new JLabel("Seuil:"));
        thresholdField = new JTextField();
        add(thresholdField);
        addButton = new JButton("Ajouter Produit");
        add(addButton);
    }

    public String getDesignation() {
        return designationField.getText();
    }

    public int getQuantity() {
        try {
            return Integer.parseInt(quantityField.getText());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public int getThreshold() {
        try {
            return Integer.parseInt(thresholdField.getText());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void addAddButtonListener(ActionListener listener) {
        addButton.addActionListener(listener);
    }
}
