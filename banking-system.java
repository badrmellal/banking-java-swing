import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BankingSystem {
    JFrame frame = new JFrame();
    CardLayout cardLayout = new CardLayout();
    JPanel mainPanel = new JPanel(cardLayout);
    JTextField username;
    JPasswordField password;
    JTextField currentBalance;
    JTextField sendBalance;
    private String loggedInUsername;
    private List<JCheckBox> userCheckBoxes = new ArrayList<>();
    private String selectedRecipientUsername = null;



    protected BankingSystem(){
    JPanel loginPanel = createLoginPanel();
    JPanel dashboardPanel = createDashboardPanel();
    JPanel registerPanel = createRegisterPanel();

    mainPanel.add(loginPanel, "login");
    mainPanel.add(dashboardPanel, "dashboard");
    mainPanel.add(registerPanel, "register");

        frame.add(mainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500,700);
        frame.setTitle("Badr Banking System");
        frame.setVisible(true);
}

    private JPanel createDashboardPanel() {
        JPanel dashboardPanel = new JPanel(null);
        JLabel dashboardTitle = new JLabel("Hi! welcome to your account.");
        dashboardTitle.setBounds(70, 50, 300, 40);
        dashboardTitle.setFont(new Font("Serif", Font.BOLD, 20));
        JLabel currentBalanceLabel = new JLabel("Your balance is: ");
        currentBalanceLabel.setBounds(50, 100, 250, 30);
        JLabel depositBalanceLabel = new JLabel("How much would you like to deposit: ");
        depositBalanceLabel.setBounds(50, 200, 250, 30);
        JTextField depositBalance = new JTextField();
        depositBalance.setBounds(50, 250, 100, 20);
        JLabel withdrawBalanceLabel = new JLabel("How much you want to withdraw: ");
        withdrawBalanceLabel.setBounds(50, 280, 250, 30);
        JTextField withdrawBalance = new JTextField();
        withdrawBalance.setBounds(50, 320, 100, 20);
        JLabel sendBalanceLabel = new JLabel("How much would you like to send: ");
        sendBalanceLabel.setBounds(50, 350, 250, 30);
        sendBalance = new JTextField();
        sendBalance.setBounds(50, 390, 100, 20);

        JPanel usersPanel = new JPanel();
        usersPanel.setBounds(50, 420, 400, 100);
        usersPanel.setLayout(new GridLayout(0, 1));

        // checkboxes dynamically
        addUsersToPanel(usersPanel);

        JButton depositButton = new JButton("Deposit");
        depositButton.setFocusable(false);
        depositButton.setBounds(250, 250, 70, 20);
        depositButton.addActionListener(new depositAmountClicked(depositBalance));
        JButton withdrawButton = new JButton("Withdraw");
        withdrawButton.setFocusable(false);
        withdrawButton.setBounds(250, 320, 70, 20);
        withdrawButton.addActionListener(new withdrawAmountClicked(withdrawBalance));
        JButton sendButton = new JButton("Send");
        sendButton.setFocusable(false);
        sendButton.setBounds(250, 390, 70, 20);
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showRecipientSelectionDialog();
            }
        });

        currentBalance = new JTextField();
        currentBalance.setBounds(50, 140, 100, 30);
        currentBalance.setFont(new Font("Serif", Font.ITALIC, 20));
        currentBalance.setEditable(false);

        dashboardPanel.add(sendButton);
        dashboardPanel.add(withdrawButton);
        dashboardPanel.add(depositButton);
        dashboardPanel.add(currentBalanceLabel);
        dashboardPanel.add(currentBalance);
        dashboardPanel.add(withdrawBalanceLabel);
        dashboardPanel.add(withdrawBalance);
        dashboardPanel.add(depositBalanceLabel);
        dashboardPanel.add(depositBalance);
        dashboardPanel.add(sendBalanceLabel);
        dashboardPanel.add(sendBalance);
        dashboardPanel.add(dashboardTitle);
        return dashboardPanel;
    }

    private void addUsersToPanel(JPanel usersPanel) {
        userCheckBoxes.clear();
        try (Connection connection = DatabaseConfig.getConnection()) {
            String query = "SELECT username FROM bankingUsers WHERE username <> ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, loggedInUsername);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String user = resultSet.getString("username");
                JCheckBox checkBox = new JCheckBox(user);
                checkBox.setActionCommand(user);
                userCheckBoxes.add(checkBox);
                usersPanel.add(checkBox);
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    private void showRecipientSelectionDialog() {
        userCheckBoxes.clear();
        JPanel recipientsPanel = new JPanel();
        recipientsPanel.setLayout(new GridLayout(0, 1));

        try (Connection connection = DatabaseConfig.getConnection()) {
            String query = "SELECT username FROM bankingUsers WHERE username <> ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, loggedInUsername);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String user = resultSet.getString("username");
                JCheckBox checkBox = new JCheckBox(user);
                checkBox.setActionCommand(user);
                userCheckBoxes.add(checkBox);
                recipientsPanel.add(checkBox);
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }

        int option = JOptionPane.showConfirmDialog(frame, recipientsPanel, "Select Recipient", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            selectedRecipientUsername = null;
            for (JCheckBox checkBox : userCheckBoxes) {
                if (checkBox.isSelected()) {
                    selectedRecipientUsername = checkBox.getActionCommand();
                    break;
                }
            }
            if (selectedRecipientUsername != null) {
                processSendAmount();
            } else {
                JOptionPane.showMessageDialog(frame, "Please select a recipient.");
            }
        }
    }

    private void processSendAmount() {
        try (Connection connection = DatabaseConfig.getConnection()) {
            double sendAmount = Double.parseDouble(sendBalance.getText());

            // Begin transaction
            connection.setAutoCommit(false);

            // Deduct from sender
            String deductQuery = "UPDATE bankingAccounts SET balance = balance - ? WHERE username = ?";
            PreparedStatement deductStatement = connection.prepareStatement(deductQuery);
            deductStatement.setDouble(1, sendAmount);
            deductStatement.setString(2, loggedInUsername);
            int rowsAffectedSender = deductStatement.executeUpdate();

            // Add to recipient
            String addQuery = "UPDATE bankingAccounts SET balance = balance + ? WHERE username = ?";
            PreparedStatement addStatement = connection.prepareStatement(addQuery);
            addStatement.setDouble(1, sendAmount);
            addStatement.setString(2, selectedRecipientUsername);
            int rowsAffectedRecipient = addStatement.executeUpdate();

            if (rowsAffectedSender > 0 && rowsAffectedRecipient > 0) {
                connection.commit();
                System.out.println("Send Successful: " + sendAmount);
                updateBalanceField();
                JOptionPane.showMessageDialog(frame, "Send Successful.");
            } else {
                connection.rollback();
                System.out.println("Send Failed: " + sendAmount);
                JOptionPane.showMessageDialog(frame, "Send Failed.");
            }

            // Reset auto-commit to true
            connection.setAutoCommit(true);

        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    private class withdrawAmountClicked implements ActionListener{
        JTextField EnteredWithdrawBalance;
        public withdrawAmountClicked(JTextField EnteredWithdrawBalance) {
            this.EnteredWithdrawBalance = EnteredWithdrawBalance;
        }
        @Override
        public void actionPerformed(ActionEvent e){
            try(Connection connection = DatabaseConfig.getConnection()){
                double withdrawNumber = Double.parseDouble(EnteredWithdrawBalance.getText());
                String queryBalance = "SELECT balance FROM bankingAccounts WHERE username = ?";
                PreparedStatement statement = connection.prepareStatement(queryBalance);
                statement.setString(1, loggedInUsername);
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    double currentLoggedBalance = resultSet.getDouble("balance");
                    if (currentLoggedBalance >= withdrawNumber) {
                        String withdrawQuery = "UPDATE bankingAccounts SET balance = balance - ? WHERE username = ?";
                        PreparedStatement withdrawStatement = connection.prepareStatement(withdrawQuery);
                        withdrawStatement.setDouble(1, withdrawNumber);
                        withdrawStatement.setString(2, loggedInUsername);
                        int rowsAffected = withdrawStatement.executeUpdate();
                        if (rowsAffected > 0) {
                            updateBalanceField();
                            JOptionPane.showMessageDialog(frame, "Withdrawal Successful.");
                        } else {
                            JOptionPane.showMessageDialog(frame, "Withdrawal Failed");
                        }
                    } else {
                        JOptionPane.showMessageDialog(frame, "Insufficient balance!");
                    }
                } else {
                    JOptionPane.showMessageDialog(frame, "Account not found!");
                }
            } catch (SQLException exception){
                exception.printStackTrace();
            }

        }
    }

    private class depositAmountClicked implements ActionListener{
        JTextField EnteredDepositBalance;
        public depositAmountClicked(JTextField EnteredDepositBalance){
            this.EnteredDepositBalance = EnteredDepositBalance;
        }
        @Override
        public void actionPerformed(ActionEvent e){
            try (Connection connection = DatabaseConfig.getConnection()) {
                double depositNumber = Double.parseDouble(EnteredDepositBalance.getText());
                String queryDeposit = "UPDATE bankingAccounts SET balance = balance + ? WHERE username = ?";
                PreparedStatement statement = connection.prepareStatement(queryDeposit);
                statement.setDouble(1, depositNumber);
                statement.setString(2, loggedInUsername);
                int rowsAffected = statement.executeUpdate();
                if (rowsAffected > 0) {
                    System.out.println("Deposit Successful: " + depositNumber);
                    updateBalanceField();
                    JOptionPane.showMessageDialog(frame, "Deposit Successful.");
                } else {
                    System.out.println("Deposit Failed: " + depositNumber);
                }

            } catch (SQLException exception){
                exception.printStackTrace();
            }
        }
    }

    private JPanel createRegisterPanel() {
        JPanel registerPanel = new JPanel(null);
        JLabel registerTitle = new JLabel("YES! you should register.");
        registerTitle.setBounds(50, 100, 250, 40);
        registerTitle.setForeground(Color.DARK_GRAY);
        registerTitle.setFont(new Font("Serif", Font.BOLD, 20));
        registerPanel.add(registerTitle);
        return registerPanel;
    }

    private JPanel createLoginPanel() {
        JPanel loginPanel = new JPanel(null);

        JButton loginButton = new JButton("Login");
        JButton registerButton = new JButton("Register");
        JLabel labelUsername = new JLabel("Username: ");
        username = new JTextField();
        JLabel labelPassword = new JLabel("Password: ");
        password = new JPasswordField();

        labelUsername.setBounds(50, 100, 200, 30);
        loginPanel.add(labelUsername);
        username.setBounds(60, 130, 200, 30);
        loginPanel.add(username);
        labelPassword.setBounds(50, 200, 200, 30);
        loginPanel.add(labelPassword);
        password.setBounds(60, 230, 200, 30);
        loginPanel.add(password);
        loginButton.setBounds(50, 300, 75, 50);
        loginPanel.add(loginButton);
        loginButton.addActionListener(new LoginButtonClicked());
        loginButton.setFocusable(false);
        registerButton.setBounds(180, 300, 75, 50);
        loginPanel.add(registerButton);
        registerButton.addActionListener(new registerButtonClicked());
        registerButton.setFocusable(false);

        return loginPanel;
    }



    private class registerButtonClicked implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
          //  cardLayout.show(mainPanel, "register");
            String enteredUsername = username.getText();
            String enteredPassword = new String(password.getPassword());
            try {
                RegistrationResult result = checkRegister(enteredUsername, enteredPassword);
                if (result.isSuccess()){
                     JOptionPane.showMessageDialog(frame, result.getMessage(), "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                     JOptionPane.showMessageDialog(frame, result.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private class LoginButtonClicked implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) {

            String enteredUsername = username.getText();
            String enteredPassword = new String(password.getPassword());

            try {
                if (checkLogin(enteredUsername, enteredPassword)){
                    loggedInUsername = enteredUsername;
                    updateBalanceField();
                     cardLayout.show(mainPanel, "dashboard");
                } else {
                    JOptionPane.showMessageDialog(frame, "Invalid username or password. Please register!");
                }
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private boolean checkLogin(String usrnm, String pass) throws SQLException {
        try (Connection connection = DatabaseConfig.getConnection()){
          String query = "SELECT * FROM bankingUsers WHERE username = ? AND password = ?";
           PreparedStatement preparedStatement = connection.prepareStatement(query);
           preparedStatement.setString(1, usrnm);
           preparedStatement.setString(2, pass);

            ResultSet resultSet = preparedStatement.executeQuery();
            loggedInUsername = usrnm;
            return resultSet.next();
        } catch (SQLException sqlException){
            sqlException.printStackTrace();
            return false;
        }
    }

    public RegistrationResult checkRegister(String urnm, String pass) throws SQLException{
        try(Connection connection = DatabaseConfig.getConnection()){
            String checkQuery = "SELECT COUNT(*) FROM bankingUsers WHERE username = ?";
            PreparedStatement statement = connection.prepareStatement(checkQuery);
            statement.setString(1, urnm);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next() && resultSet.getInt(1)>0){
                return new  RegistrationResult(false, "username already exist!");
            }

            String query = "INSERT INTO bankingUsers (username, password) VALUES (?, ?)";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, urnm);
            preparedStatement.setString(2, pass);
            int rowAffected = preparedStatement.executeUpdate();
            if (rowAffected>0){
                String createAccountQuery = "INSERT INTO bankingAccounts (username, balance) VALUES (?, 0)";
                PreparedStatement createAccountStatement = connection.prepareStatement(createAccountQuery);
                createAccountStatement.setString(1, urnm);
                createAccountStatement.executeUpdate();
                return new RegistrationResult(true, "Account successfully created.");
            } else {
                return new  RegistrationResult(false, "An error has occurred!");
            }

        }
    }

    private void updateBalanceField() {
        try (Connection connection = DatabaseConfig.getConnection()) {
            String sql = "SELECT balance FROM bankingAccounts WHERE username = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, loggedInUsername);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                currentBalance.setText(String.valueOf(rs.getDouble("balance")));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


}
