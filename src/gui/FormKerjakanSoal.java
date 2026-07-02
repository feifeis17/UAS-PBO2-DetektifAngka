/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package gui;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.Timer;
/**
 *
 * @author feisa
 */
public class FormKerjakanSoal extends javax.swing.JFrame {
String penggunaAktif = "ayumi"; 
    String namaPenggunaAktif = "Ayumi";
    List<Soal> listSoal = new ArrayList<>();
    int indeksSoalSekarang = 0;
    int skorTotal = 0;
    
    // Stats for the current operator group
    int soalBenarOperatorIni = 0;
    int soalSalahOperatorIni = 0;
    int durasiOperatorIni = 0;
    
    // Total duration across all operations
    int durasiTotalSemua = 0; 
    
    Timer timerHitungMundur;
    int sisaWaktu;
    final int WAKTU_MAKS = 60; // 60 seconds per question

    // Object to hold question data
    class Soal {
        String id;
        String operator;
        int nomor;
        int bil1;
        int bil2;
        int jawaban;

        public Soal(String id, String operator, int nomor, int bil1, int bil2, int jawaban) {
            this.id = id;
            this.operator = operator;
            this.nomor = nomor;
            this.bil1 = bil1;
            this.bil2 = bil2;
            this.jawaban = jawaban;
        }
    }
    /**
     * Creates new form FormKerjakanSoal
     */
    public FormKerjakanSoal() {
        initComponents();
        jPanel1.putClientProperty("JComponent.roundRect", true);
        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Semua Operasi", "Tambah (+)", "Kurang (-)", "Kali (*)", "Bagi (/)" }));
        jComboBox1.setEnabled(true);
        
        this.pack();
        this.setResizable(false);
        this.setLocationRelativeTo(null);

        tarikSoalDariDatabase();
    }
private void tarikSoalDariDatabase() {
        try {
            Connection conn = config.koneksi.koneksiDB();
            Statement st = conn.createStatement();
            String pilihan = jComboBox1.getSelectedItem().toString();
            String sql = "SELECT * FROM soal_dinamis WHERE pengguna = '" + penggunaAktif + "'";
            if (pilihan.contains("+")) sql += " AND operator = '+'";
            else if (pilihan.contains("-")) sql += " AND operator = '-'";
            else if (pilihan.contains("*")) sql += " AND operator = '*'";
            else if (pilihan.contains("/")) sql += " AND operator = '/'";
            
            sql += " ORDER BY operator ASC, nomor ASC";
            
            ResultSet rs = st.executeQuery(sql);
            
            listSoal.clear();
            while (rs.next()) {
                listSoal.add(new Soal(
                    rs.getString("id"),
                    rs.getString("operator"),
                    rs.getInt("nomor"),
                    rs.getInt("bilangan_1"),
                    rs.getInt("bilangan_2"),
                    rs.getInt("jawaban")
                ));
            }
            
            if (listSoal.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Belum ada soal! Mentor harus men-generate soal terlebih dahulu.");
                kembaliKeDashboard();
            } else {
                mulaiKuis();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal mengambil soal: " + e.getMessage());
        }
    }

    private void mulaiKuis() {
        indeksSoalSekarang = 0;
        skorTotal = 0;
        durasiTotalSemua = 0;
        resetStatistikOperator();
        inisialisasiTimer();
        tampilkanSoalLayar();
    }

    private void resetStatistikOperator() {
        soalBenarOperatorIni = 0;
        soalSalahOperatorIni = 0;
        durasiOperatorIni = 0;
    }

    private String getNamaOperator(String opSymbol) {
        switch (opSymbol) {
            case "+": return "Tambah (+)";
            case "-": return "Kurang (-)";
            case "*": return "Kali (x)";
            case "/": return "Bagi (÷)";
            default: return opSymbol;
        }
    }

    private void tampilkanSoalLayar() {
        if (indeksSoalSekarang >= listSoal.size()) {
            selesaikanKuis();
            return;
        }

        Soal soalSekarang = listSoal.get(indeksSoalSekarang);
        
        // Detect if we shifted to a new operator group (e.g., from + to -)
        if (indeksSoalSekarang > 0) {
             Soal soalSebelumnya = listSoal.get(indeksSoalSekarang - 1);
             if (!soalSekarang.operator.equals(soalSebelumnya.operator)) {
                 // Save the previous operator's stats before resetting
                 simpanStatistikKeDatabase(soalSebelumnya.operator);
                 resetStatistikOperator();
             }
        }

        // Update Labels
        lblPoin.setText(String.valueOf(skorTotal));
        lblBerkas.setText("Berkas Kasus #" + (indeksSoalSekarang + 1) + " / " + listSoal.size());
        
        // Format the equation string. E.g., "48 + 39 = ?"
        jLabel4.setText("\"" + soalSekarang.bil1 + " " + soalSekarang.operator + " " + soalSekarang.bil2 + " = ?\"");
        
        lblInfoSoal.setText("Soal ke-" + (indeksSoalSekarang + 1) + " dari " + listSoal.size() + 
                            " | " + getNamaOperator(soalSekarang.operator) + " " + soalSekarang.nomor + "/10");
                            
        lblInfoSkor.setText("Skor sementara: " + skorTotal + 
                            " | " + getNamaOperator(soalSekarang.operator) + " benar: " + soalBenarOperatorIni + "/10");

        // Reset Input and Timer
        txtJawaban.setText("");
        txtJawaban.requestFocus();
        sisaWaktu = WAKTU_MAKS;
        updateLabelTimer();
        timerHitungMundur.start();
    }

    private void inisialisasiTimer() {
        timerHitungMundur = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sisaWaktu--;
                updateLabelTimer();
                
                if (sisaWaktu <= 0) {
                    timerHitungMundur.stop();
                    prosesJawaban(true); // true = forced timeout
                }
            }
        });
    }

    private void updateLabelTimer() {
        lblAngkaTimer.setText(String.valueOf(sisaWaktu));
        lblTimer.setText("Waktu: " + sisaWaktu + " detik");
        
        if (sisaWaktu <= 10) {
            lblAngkaTimer.setForeground(Color.RED);
            lblTimer.setForeground(Color.RED);
        } else {
            lblAngkaTimer.setForeground(Color.BLACK);
            lblTimer.setForeground(new Color(0, 153, 51)); // Greenish
        }
    }

    private void simpanStatistikKeDatabase(String operatorTerakhir) {
         try {
            Connection conn = config.koneksi.koneksiDB();
            Statement st = conn.createStatement();
            
            String tgl = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String jam = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            int skorGrupIni = soalBenarOperatorIni * 10;
            
            String sql = "INSERT INTO pengerjaan_soal (pengguna, nama, operator, benar, salah, skor, waktu, tanggal_pengerjaan, jam_pengerjaan) " +
                         "VALUES ('" + penggunaAktif + "', '" + namaPenggunaAktif + "', '" + operatorTerakhir + "', " + 
                         soalBenarOperatorIni + ", " + soalSalahOperatorIni + ", " + skorGrupIni + ", " + durasiOperatorIni + ", " +
                         "'" + tgl + "', '" + jam + "')";
            st.executeUpdate(sql);
            
         } catch (Exception e) {
             System.out.println("Gagal menyimpan rekap per operator: " + e.getMessage());
         }
    }

    private void prosesJawaban(boolean isTimeout) {
        timerHitungMundur.stop();
        Soal soalSekarang = listSoal.get(indeksSoalSekarang);
        
        int waktuDihabiskan = WAKTU_MAKS - sisaWaktu;
        durasiOperatorIni += waktuDihabiskan;
        durasiTotalSemua += waktuDihabiskan;

        if (isTimeout) {
            soalSalahOperatorIni++;
            JOptionPane.showMessageDialog(this, "Waktu Habis! Jawaban yang benar adalah: " + soalSekarang.jawaban, "Timeout", JOptionPane.WARNING_MESSAGE);
        } else {
            try {
                int jawabanUser = Integer.parseInt(txtJawaban.getText().trim());
                if (jawabanUser == soalSekarang.jawaban) {
                    soalBenarOperatorIni++;
                    skorTotal += 10;
                    JOptionPane.showMessageDialog(this, "Benar! +10 Poin", "Tepat!", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    soalSalahOperatorIni++;
                    JOptionPane.showMessageDialog(this, "Salah! Jawaban yang benar adalah: " + soalSekarang.jawaban, "Keliru", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException e) {
                // If they typed letters or left it blank
                soalSalahOperatorIni++;
                JOptionPane.showMessageDialog(this, "Input tidak valid! Dianggap Salah. Jawaban: " + soalSekarang.jawaban, "Keliru", JOptionPane.ERROR_MESSAGE);
            }
        }

        indeksSoalSekarang++;
        tampilkanSoalLayar();
    }

    private void selesaikanKuis() {
        // Save the very last operator group's stats
        if (!listSoal.isEmpty()) {
            Soal soalTerakhir = listSoal.get(listSoal.size() - 1);
            simpanStatistikKeDatabase(soalTerakhir.operator);
        }
        
        JOptionPane.showMessageDialog(this, 
            "MISI SELESAI!\n\nSkor Total: " + skorTotal + "\nDurasi Total: " + durasiTotalSemua + " detik", 
            "Selamat!", JOptionPane.INFORMATION_MESSAGE);
            
        kembaliKeDashboard();
    }
    private void kembaliKeDashboard() {
        if (timerHitungMundur != null) {
            timerHitungMundur.stop();
        }
        new dashboardlvl2(penggunaAktif, namaPenggunaAktif).setVisible(true);
        this.dispose();
    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jComboBox1 = new javax.swing.JComboBox<>();
        jPanel2 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        lblPoin = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        lblInfoSoal = new javax.swing.JLabel();
        lblInfoSkor = new javax.swing.JLabel();
        lblTimer = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        lblAngkaTimer = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        lblBerkas = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        txtJawaban = new javax.swing.JTextField();
        btnJawab = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(204, 204, 204));

        jPanel3.setBackground(new java.awt.Color(251, 243, 223));
        jPanel3.setForeground(new java.awt.Color(251, 243, 223));

        jPanel4.setBackground(new java.awt.Color(33, 31, 84));
        jPanel4.setForeground(new java.awt.Color(33, 31, 84));

        jLabel16.setIcon(new javax.swing.ImageIcon(getClass().getResource("/gambar/Mini.png"))); // NOI18N

        jLabel17.setIcon(new javax.swing.ImageIcon(getClass().getResource("/gambar/Maxi.png"))); // NOI18N

        jLabel18.setIcon(new javax.swing.ImageIcon(getClass().getResource("/gambar/Close.png"))); // NOI18N

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addComponent(jLabel16)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel17)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel18)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel18)
                    .addComponent(jLabel17)
                    .addComponent(jLabel16))
                .addContainerGap())
        );

        jLabel1.setText("Operator");

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jComboBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox1ActionPerformed(evt);
            }
        });

        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/gambar/icons8-star-12.png"))); // NOI18N

        lblPoin.setText("-");

        jLabel3.setText("poin");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblPoin)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3)
                .addContainerGap(26, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lblPoin)
                        .addComponent(jLabel3))
                    .addComponent(jLabel2))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        lblInfoSoal.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblInfoSoal.setText("Soal ke-1 dari 40 | Tambah (+) 1/10");

        lblInfoSkor.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblInfoSkor.setText("Skor sementara: 0 | Tambah (+) benar: 0/10");

        lblTimer.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblTimer.setText("Waktu: 60 detik");

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lblInfoSoal, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(lblTimer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addComponent(lblInfoSkor, javax.swing.GroupLayout.PREFERRED_SIZE, 749, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblInfoSoal)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblInfoSkor)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblTimer)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        lblAngkaTimer.setFont(new java.awt.Font("Segoe UI", 1, 48)); // NOI18N
        lblAngkaTimer.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblAngkaTimer.setText("60");

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lblAngkaTimer, javax.swing.GroupLayout.DEFAULT_SIZE, 160, Short.MAX_VALUE)
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lblAngkaTimer, javax.swing.GroupLayout.DEFAULT_SIZE, 100, Short.MAX_VALUE)
        );

        lblBerkas.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblBerkas.setText("Berkas Kasus #1 / 40");

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 62)); // NOI18N
        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel4.setText("\"48 + 39 = ?\"");

        txtJawaban.setFont(new java.awt.Font("Segoe UI", 1, 36)); // NOI18N
        txtJawaban.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        txtJawaban.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txtJawabanKeyPressed(evt);
            }
        });

        btnJawab.setBackground(new java.awt.Color(0, 204, 0));
        btnJawab.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        btnJawab.setText("Jawab");
        btnJawab.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnJawabActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lblBerkas, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addGap(189, 189, 189)
                .addComponent(txtJawaban, javax.swing.GroupLayout.PREFERRED_SIZE, 167, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnJawab, javax.swing.GroupLayout.PREFERRED_SIZE, 174, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblBerkas)
                .addGap(18, 18, 18)
                .addComponent(jLabel4)
                .addGap(18, 18, 18)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(txtJawaban)
                    .addComponent(btnJawab, javax.swing.GroupLayout.DEFAULT_SIZE, 72, Short.MAX_VALUE))
                .addContainerGap(77, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 145, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(16, 16, 16))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(305, 305, 305))
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())
                    .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel1)
                        .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 34, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(14, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnJawabActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnJawabActionPerformed
        prosesJawaban(false);
    }//GEN-LAST:event_btnJawabActionPerformed

    private void txtJawabanKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtJawabanKeyPressed
        if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
            prosesJawaban(false);
        }
    }//GEN-LAST:event_txtJawabanKeyPressed

    private void jComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1ActionPerformed
        if (timerHitungMundur != null) {
            timerHitungMundur.stop();
        }
        tarikSoalDariDatabase(); 
        txtJawaban.requestFocus();
    }//GEN-LAST:event_jComboBox1ActionPerformed

    /**
     * @param args the command line arguments
     */
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnJawab;
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JLabel lblAngkaTimer;
    private javax.swing.JLabel lblBerkas;
    private javax.swing.JLabel lblInfoSkor;
    private javax.swing.JLabel lblInfoSoal;
    private javax.swing.JLabel lblPoin;
    private javax.swing.JLabel lblTimer;
    private javax.swing.JTextField txtJawaban;
    // End of variables declaration//GEN-END:variables
}
