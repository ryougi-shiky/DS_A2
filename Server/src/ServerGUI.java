import javax.swing.*;
import java.awt.*;
import javax.swing.JScrollPane;
import java.awt.event.*;
import java.rmi.RemoteException;
import java.lang.NumberFormatException;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.awt.geom.*;

public class ServerGUI {
    private static ServerInterface server;
    private static JFrame frame;
    private JList<String> clientList;
    private DefaultListModel<String> clientListModel;
    private static int serverPortNumber;
    private static String managerName;
    public whiteBoard whiteBoard;
    private static Color[] colors = {Color.BLACK, Color.BLUE, Color.CYAN, Color.DARK_GRAY, Color.GRAY, Color.GREEN,
            Color.LIGHT_GRAY, Color.MAGENTA, Color.ORANGE, Color.PINK, Color.RED, Color.YELLOW,
            new Color(147, 112, 219), new Color(50, 205, 50), new Color(0, 191, 255), new Color(139, 69, 19)};

    public ServerGUI() {
        frame = new JFrame("Shared Board White Server");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 650);
        frame.setLayout(new BorderLayout());

        // Add a "File" menu
        TopBarMenu();

        frame.add(new JLabel(" Connected Clients List: "), BorderLayout.NORTH);

        // Add the server info panel
        JPanel serverInfoPanel = new JPanel(new GridLayout(3, 1));
        serverInfoPanel.add(new JLabel(" Server Address: 127.0.0.1 "));
        serverInfoPanel.add(new JLabel(" Port: " + serverPortNumber));
        serverInfoPanel.add(new JLabel(" Manager Name: " + managerName));
        frame.add(serverInfoPanel, BorderLayout.SOUTH);

        // Display the joined clients list
        clientListModel = new DefaultListModel<>();
        clientList = new JList<>(clientListModel);
        JScrollPane scrollPane = new JScrollPane(clientList);
        frame.add(scrollPane, BorderLayout.CENTER);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                frame.dispose();
                try {
                    server.closeServer();
                    System.out.println("Server notify server remote object to every client to close window");
                } catch (RemoteException re) {
                    re.printStackTrace();
                }
                System.exit(0);
            }
        });

        // "Kick Out" menu option
        JPopupMenu rightClickMenu = new JPopupMenu();
        JMenuItem kickOutOption = new JMenuItem("Kick Out");
        kickOutOption.addActionListener(e -> {
            kickOut();
        });
        rightClickMenu.add(kickOutOption);

        clientList.addMouseListener(new MouseAdapter() {
            @Override // Mouse right click the client, display the "Kick out" option
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int index = clientList.locationToIndex(e.getPoint());
                    if (index != -1) {
                        clientList.setSelectedIndex(index);
                        rightClickMenu.show(clientList, e.getX(), e.getY());
                    }
                }
            }
        });

        managerWhiteBoard();

        frame.setVisible(true);
    }

    private void fileSelect(String option) {
        System.out.println("file option: " + option);
        try {
            server.fileSelect(option);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void kickOut() {
        // Mouse select a client in the list
        int selectedIndex = clientList.getSelectedIndex();
        if (selectedIndex != -1) {
            String clientName = clientListModel.get(selectedIndex);

            try {
                boolean ifDel = server.kickout(clientName);
                if (ifDel) {
                    removeClient(clientName);
                } else {
                    System.out.println("The client " + clientName + " does not exist");
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    // Initialise the setting of the server.
    // When open the program, pop up a window to enter the port number that the server will run on
    public static int setup() {
        JTextField usernameField = new JTextField(15);
        JTextField portNumberField = new JTextField("50000", 15);

        JPanel serverStartPanel = new JPanel(new GridLayout(0, 1));
        serverStartPanel.add(new JLabel("Welcome to shared whiteboard!"));
        serverStartPanel.add(new JLabel("Create your whiteboard server"));
        serverStartPanel.add(new JLabel("Please set the server port (50000-65535):"));
        serverStartPanel.add(portNumberField);
        serverStartPanel.add(new JLabel("Username:"));
        serverStartPanel.add(usernameField);

        int portNumber;


        while (true) {
            int result = JOptionPane.showConfirmDialog(null, serverStartPanel, "Start a Server", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                managerName = usernameField.getText();
                try { // Check if contains invalid characters
                    portNumber = Integer.parseInt(portNumberField.getText());
                } catch (NumberFormatException e) {
                    setupError("Invalid port number! Please try again");
                    e.printStackTrace();
                    continue;
                }
                if (managerName.trim().isEmpty()){
                    setupError("Manager username cannot be blank! Please try again");
                    continue;
                }
                if (portNumber < 50000 || portNumber > 65535) {
                    JOptionPane.showMessageDialog(null, "Please enter a port number 50000 - 65535!",
                            "Invalid Port Number", JOptionPane.ERROR_MESSAGE);
                } else { // setup success
                    serverPortNumber = portNumber;
                    return portNumber;
                }
            } else {
                System.exit(0);
                return 0;
            }
        }
    }

    public void managerWhiteBoard() {
        // Create whiteboard on the right side.
        whiteBoard = new whiteBoard();
        frame.add(whiteBoard, BorderLayout.EAST);
//        JPanel whiteboardPanel = new JPanel();
//        frame.add(whiteboardPanel, BorderLayout.EAST);
//        whiteboardPanel.setPreferredSize(new Dimension(700, 600));


    }

    public String getManagerName() {
        return managerName;
    }

    public void addClient(String clientName) {
        clientListModel.addElement(clientName);
    }

    public void removeClient(String clientName) {
        clientListModel.removeElement(clientName);
    }

    public void setServerInterface(ServerInterface server) {
        this.server = server;
    }

    public static void setupError(String err) {
        JOptionPane.showMessageDialog(null, err, "Login Error", JOptionPane.ERROR_MESSAGE);
    }

    private Image createColorIcon(Color color) {
        BufferedImage colorIcon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        Graphics2D board = colorIcon.createGraphics();
        board.setColor(color);
        board.fillRect(0, 0, 16, 16);
        board.dispose();
        return colorIcon;
    }

    private void TopBarMenu() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        String[] files = {"Save", "Save As", "Open", "New", "Close"};
        for (String file : files) {
            JMenuItem fileOption = new JMenuItem(file);
            fileOption.addActionListener(e -> fileSelect(file));
            fileMenu.add(fileOption);
        }
        menuBar.add(fileMenu);

        JMenu shapesMenu = new JMenu("Shapes");
        String[] shapes = {"Line", "Circle", "Oval", "Rectangle"};
        for (String shape : shapes) {
            JMenuItem menuItem = new JMenuItem(shape);
            menuItem.addActionListener(e -> whiteBoard.setShape(shape));
            shapesMenu.add(menuItem);
        }
        menuBar.add(shapesMenu);

        JMenu colorsMenu = new JMenu("Colors");
        for (Color color : colors) {
            JMenuItem menuItem = new JMenuItem(new ImageIcon(createColorIcon(color)));
            menuItem.addActionListener(e -> whiteBoard.setColor(color));
            colorsMenu.add(menuItem);
        }
        menuBar.add(colorsMenu);

        JMenu textMenu = new JMenu("Text");
        JMenuItem menuItem = new JMenuItem("Add Text Box");
        menuItem.addActionListener(e -> whiteBoard.enableTextBox());
        textMenu.add(menuItem);
        menuBar.add(textMenu);

        frame.setJMenuBar(menuBar);
    }

    class whiteBoard extends JPanel {
        // Display current partial drawing tracks
        public Shape currentDrawing; // The shape is currently drawing. If null, currently not drawing.
        public Color currentColor = Color.BLACK;
        public String currentShape = "Line";
        private int x, y; // Mouse position
        private int width, height; // Used for drawing shapes. Computed by mouse position
        // store drawn shapes , positions and corresponding colours
        public ArrayList<Object> shapes;
        public ArrayList<Color> colors;
        public ArrayList<Point> shapePositions;
        // Temporarily draw partial shapes
        public Shape syncPartialDrawing;
        public Color syncPartialColor;
        public String syncPartialShape;
        private JTextField textBox;
        private boolean textBoxEnabled = false;

        public whiteBoard() {
            setPreferredSize(new Dimension(700, 600));
            setBackground(Color.WHITE);

            shapes = new java.util.ArrayList<>();
            colors = new java.util.ArrayList<>();
            shapePositions = new java.util.ArrayList<>();

            syncPartialDrawing = null;
            syncPartialColor = null;
            syncPartialShape = null;

            MouseAdapter mouseAdapter = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    x = e.getX();
                    y = e.getY();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    clearPartialShapes();
                    drawShape(e);
                    shapes.add(currentDrawing);
                    colors.add(currentColor);
                    shapePositions.add(new Point(x, y));
                    currentDrawing = null;
                    repaint();
                    drawToServer();
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    drawShape(e);
                    drawPartialToServer(currentDrawing, currentColor, currentShape);
                    repaint();
                }
            };

            addMouseListener(mouseAdapter);
            addMouseMotionListener(mouseAdapter);


            textBox = new JTextField();
            textBox.setBounds(0, 0, 100, 20);
            textBox.setVisible(false); // Hide text box until user need to use text box
            textBox.addActionListener(e -> {
                textBox.setVisible(false); // Finish enter text, set text box back to invisible
                textBoxEnabled = false;
                // Calculate the baseline position for the text box
                FontMetrics fm = textBox.getFontMetrics(textBox.getFont());

                // Only store the text string and its position, not the text box object itself
                shapes.add(textBox.getText());
                shapePositions.add(new Point(textBox.getX(), (textBox.getY() + fm.getAscent())));
                colors.add(currentColor);
                textBox.setText("");
                repaint();
                drawToServer();
            });
            add(textBox);
        }

        // add a partial shape
        public void addPartialShape(Shape curDrawing, Color curColor, String curShape) {
            syncPartialDrawing = curDrawing;
            syncPartialColor = curColor;
            syncPartialShape = curShape;

            repaint();
        }

        // clear partial shapes after mouse released
        public void clearPartialShapes() {
            syncPartialDrawing = null;
            syncPartialColor = null;
            syncPartialShape = null;
            repaint();
        }


        private void drawShape(MouseEvent e) {
            width = e.getX() - x;
            height = e.getY() - y;

            switch (currentShape) {
                case "Line":
                    currentDrawing = new java.awt.geom.Line2D.Float(x, y, e.getX(), e.getY());
                    break;
                case "Circle":
                    int diameter = Math.max(Math.abs(width), Math.abs(height));
                    currentDrawing = new java.awt.geom.Ellipse2D.Float(
                            x - (width < 0 ? diameter : 0),
                            y - (height < 0 ? diameter : 0),
                            diameter, diameter);
                    break;
                case "Oval":
                    currentDrawing = new java.awt.geom.Ellipse2D.Float(
                            x - (width < 0 ? -width : 0),
                            y - (height < 0 ? -height : 0),
                            Math.abs(width), Math.abs(height));
                    break;
                case "Rectangle":
                    currentDrawing = new Rectangle(
                            x - (width < 0 ? -width : 0),
                            y - (height < 0 ? -height : 0),
                            Math.abs(width), Math.abs(height));
                    break;
                case "Text":
                    if (textBoxEnabled) {
                        textBox.setLocation(e.getX(), e.getY());
                        textBox.setVisible(true);
                        textBox.requestFocus();
                    }
                    break;
            }
        }

        // paintComponent is called when invoking repaint()
        @Override
        protected void paintComponent(Graphics board) {
            // Ensure the original painting operations are executed
            super.paintComponent(board);
            Graphics2D board2D = (Graphics2D) board;

            // Draw the all shapes and text boxes
            for (int i = 0; i < shapes.size(); i++) {
                board2D.setColor(colors.get(i));
                if (shapes.get(i) instanceof Shape) {
                    board2D.draw((Shape) shapes.get(i));
                } else if (shapes.get(i) instanceof String) { // If it's text box
                    board2D.drawString((String) shapes.get(i), (int) shapePositions.get(i).getX(), (int) shapePositions.get(i).getY());
                }
            }

            // Draw the partial shapes
            if (syncPartialDrawing != null) {
                board2D.setColor(syncPartialColor);
                board2D.draw(syncPartialDrawing);
            }

            // Display the current drawing shape
            if (currentDrawing != null) {
                board2D.setColor(currentColor);
                board2D.draw(currentDrawing);
            }
        }

        private void drawToServer() {
            try {
                server.draw(null);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        private void drawPartialToServer(Shape curDrawing, Color curColor, String curShape) {
            try {
                server.partialDraw(null, curDrawing, curColor, curShape);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        public void enableTextBox() {
            textBoxEnabled = true;
        }

        // Text box will follow the mouse motion
        @Override
        protected void processMouseMotionEvent(MouseEvent e) {
            super.processMouseMotionEvent(e);
            if (textBoxEnabled) {
                textBox.setLocation(e.getX(), e.getY());
                textBox.setVisible(true);
            }
        }

        public void setShape(String shape) {
            currentShape = shape;
        }

        public void setColor(Color color) {
            currentColor = color;
        }
    }

    public void updateBoardStatus(ArrayList<Object> shapes, ArrayList<Color> colors, ArrayList<Point> shapePositions) {
        this.whiteBoard.shapes = shapes;
        this.whiteBoard.colors = colors;
        this.whiteBoard.shapePositions = shapePositions;
        this.whiteBoard.repaint();
    }

    public void updatePartialDraw(Shape curDrawing, Color curColor, String curShape) {
        this.whiteBoard.addPartialShape(curDrawing, curColor, curShape);
    }

    public void clear() {
        // Reset all values
        this.whiteBoard.shapes.clear();
        this.whiteBoard.colors.clear();
        this.whiteBoard.shapePositions.clear();

        this.whiteBoard.syncPartialDrawing = null;
        this.whiteBoard.syncPartialColor = null;
        this.whiteBoard.syncPartialShape = null;

        this.whiteBoard.currentDrawing = null;
        this.whiteBoard.currentColor = Color.BLACK;
        this.whiteBoard.currentShape = "Line";

        this.whiteBoard.repaint();

//        JDialog dialog = new JDialog(this, "Notification", false);
//        dialog.setSize(250, 100);
//        dialog.setLocationRelativeTo(this);
//        dialog.getContentPane().setLayout(new BoxLayout(dialog.getContentPane(), BoxLayout.Y_AXIS));
//        dialog.getContentPane().add(new JLabel(" The server open or create a new board."));
//        dialog.getContentPane().add(new JLabel(" This is a new board now."));
//        dialog.setVisible(true);
    }
}