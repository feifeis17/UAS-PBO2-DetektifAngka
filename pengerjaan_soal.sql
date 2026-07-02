-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Waktu pembuatan: 02 Jul 2026 pada 16.45
-- Versi server: 10.4.32-MariaDB-log
-- Versi PHP: 8.0.30

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `pis12noliga`
--

-- --------------------------------------------------------

--
-- Struktur dari tabel `pengerjaan_soal`
--

CREATE TABLE `pengerjaan_soal` (
  `id_pengerjaan` int(11) NOT NULL,
  `pengguna` varchar(20) DEFAULT NULL,
  `operator` varchar(5) DEFAULT NULL,
  `benar` int(11) DEFAULT NULL,
  `salah` int(11) DEFAULT NULL,
  `skor` int(11) DEFAULT NULL,
  `waktu` int(11) DEFAULT NULL,
  `tanggal_pengerjaan` date DEFAULT NULL,
  `jam_pengerjaan` time DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Indexes for dumped tables
--

--
-- Indeks untuk tabel `pengerjaan_soal`
--
ALTER TABLE `pengerjaan_soal`
  ADD PRIMARY KEY (`id_pengerjaan`);

--
-- AUTO_INCREMENT untuk tabel yang dibuang
--

--
-- AUTO_INCREMENT untuk tabel `pengerjaan_soal`
--
ALTER TABLE `pengerjaan_soal`
  MODIFY `id_pengerjaan` int(11) NOT NULL AUTO_INCREMENT;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
