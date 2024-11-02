package huffman.main;

import huffman.tree.Leaf;
import huffman.tree.Node;
import huffman.tree.Tree;

import java.io.*;
import java.util.*;

public class Main {
    public static HashMap<Character, String> huffmanMain(List<Symbol> symbolList) {
        long startTime = System.nanoTime();

        PriorityQueue<Node> pq = new PriorityQueue<>();

        // Partie 1 - chargement des symboles avec leurs fréquences
        Map<Character, Integer> frequencyMap = new HashMap<>();
        for (Symbol symbol : symbolList) {
            frequencyMap.merge(symbol.symbol(), symbol.frequency(), Integer::sum);
        }
        frequencyMap.forEach((key, value) -> pq.add(new Leaf(key, value)));

        // Cas spécial : un seul caractère unique
        if (pq.size() == 1) {
            Symbol dummy = new Symbol('\0', 0);
            pq.add(new Leaf(dummy.symbol(), 0));
        }

        // Partie 2 - création de l'arbre
        while (pq.size() > 1) {
            Node left = pq.poll();
            Node right = pq.poll();
            assert right != null;
            pq.add(new Tree(left, right));
        }

        // Partie 3 - création de la table de codes
        Tree huffmanTree = (Tree) pq.peek();
        assert huffmanTree != null;

        long endTime = System.nanoTime();
        System.out.println("Durée d'exécution : " + (endTime - startTime) / 1_000_000 + " ms");

        return huffmanTree.createTable();
    }

    public static void writeIn(String fileName, List<Symbol> symbolList) {
        var codes = huffmanMain(symbolList);
        saveHuffmanTree(fileName, codes);

        StringBuilder codeJson = new StringBuilder(" { ");
        codes.forEach((key, value) -> {
            codeJson.append("\"").append(key).append("\"").append(" : ").append("\"").append(value).append("\"").append(", ");
        });
        if (codeJson.length() > 2) {
            codeJson.setLength(codeJson.length() - 2);
        }
        codeJson.append(" }");

        try (PrintWriter writer = new PrintWriter(new FileWriter("JSON/" + fileName + ".json"))) {
            writer.println(codeJson.toString());
        } catch (IOException e) {
            System.err.println("Erreur lors de l'écriture du fichier JSON: " + e.getMessage());
        }
    }

    private static void saveHuffmanTree(String fileName, HashMap<Character, String> codes) {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream("Trees/" + fileName + ".tree"))) {
            oos.writeObject(codes);
        } catch (IOException e) {
            System.err.println("Erreur lors de la sauvegarde de l'arbre: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static HashMap<Character, String> loadHuffmanTree(String fileName) {
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream("Trees/" + fileName + ".tree"))) {
            return (HashMap<Character, String>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erreur lors du chargement de l'arbre: " + e.getMessage());
            return null;
        }
    }

    public static String readIn(String fileName) {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader("Input/" + fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la lecture du fichier: " + e.getMessage());
        }
        return content.toString();
    }

    public static void compressToFile(String input, String outputFileName, HashMap<Character, String> codes) {
        StringBuilder encodedText = new StringBuilder();
        for (char c : input.toCharArray()) {
            String code = codes.get(c);
            if (code == null) {
                System.err.println("Caractère non trouvé dans la table de codes: " + c);
                return;
            }
            encodedText.append(code);
        }

        try (DataOutputStream dos = new DataOutputStream(
                new FileOutputStream("Compressed/" + outputFileName))) {

            dos.writeInt(input.length());

            dos.writeInt(encodedText.length());

            byte[] bytes = convertToBytes(encodedText.toString());
            dos.write(bytes);

            System.out.println("Fichier compressé créé: Compressed/" + outputFileName);
        } catch (IOException e) {
            System.err.println("Erreur lors de la compression: " + e.getMessage());
        }
    }

    private static byte[] convertToBytes(String binaryString) {
        int byteSize = (binaryString.length() + 7) / 8;
        byte[] bytes = new byte[byteSize];
        for (int i = 0; i < binaryString.length(); i++) {
            if (binaryString.charAt(i) == '1') {
                bytes[i / 8] |= (byte) (1 << (7 - (i % 8)));
            }
        }
        return bytes;
    }

    public static String decompressFromFile(String inputFileName, String treeFileName) {
        // Charger l'arbre de Huffman
        HashMap<Character, String> codes = loadHuffmanTree(treeFileName);
        if (codes == null) return "";

        // Créer la table de décodage inversée
        HashMap<String, Character> decodeMap = new HashMap<>();
        codes.forEach((character, code) -> decodeMap.put(code, character));

        try (DataInputStream dis = new DataInputStream(
                new FileInputStream("Compressed/" + inputFileName))) {
            // Lire les métadonnées
            int originalLength = dis.readInt();
            int bitLength = dis.readInt();

            // lire les donnees compressees
            byte[] bytes = dis.readAllBytes();
            String binaryString = convertBytesToBinaryString(bytes, bitLength);

            // Decompresser
            StringBuilder decodedText = new StringBuilder();
            StringBuilder currentCode = new StringBuilder();

            for (int i = 0; i < binaryString.length() && decodedText.length() < originalLength; i++) {
                currentCode.append(binaryString.charAt(i));
                Character decodedChar = decodeMap.get(currentCode.toString());

                if (decodedChar != null) {
                    decodedText.append(decodedChar);
                    currentCode.setLength(0);
                }
            }

            // sauvegarder le texte décompresse dans un fichier
            String decompressedText = decodedText.toString();
            saveDecompressedText(inputFileName, decompressedText);

            return decompressedText;
        } catch (IOException e) {
            System.err.println("Erreur lors de la décompression: " + e.getMessage());
            return "";
        }
    }

    private static String convertBytesToBinaryString(byte[] bytes, int bitLength) {
        StringBuilder binaryString = new StringBuilder();
        for (int i = 0; i < bitLength; i++) {
            byte b = bytes[i / 8];
            binaryString.append((b >> (7 - (i % 8)) & 1));
        }
        return binaryString.toString();
    }

    // pour sauvegarder le texte décompressé
    private static void saveDecompressedText(String originalFileName, String decompressedText) {

        String decompressedFileName = "Decompressed/decompressed_" + originalFileName.replace(".bin", ".txt");

        try (PrintWriter writer = new PrintWriter(new FileWriter(decompressedFileName))) {
            writer.write(decompressedText);
            System.out.println("Fichier décompressé sauvegardé dans: " + decompressedFileName);
        } catch (IOException e) {
            System.err.println("Erreur lors de la sauvegarde du fichier décompressé: " + e.getMessage());
        }
    }

    // Nouvelle fonction pour créer tous les dossiers nécessaires
    private static void createRequiredDirectories() {
        List<String> directories = Arrays.asList(
                "Input",
                "Compressed",
                "Decompressed",
                "Trees",
                "JSON"
        );

        for (String dir : directories) {
            File directory = new File(dir);
            if (!directory.exists()) {
                boolean created = directory.mkdir();
                if (created) {
                    System.out.println("Dossier créé: " + dir);
                }
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("Démarrage du programme de compression Huffman");


        String inputFileName = "test.txt";
        String compressedFileName = "test.bin";
        String treeFileName = "test";

        System.out.println("\nLecture du fichier d'entrée: " + inputFileName);
        String textInput = readIn(inputFileName);
        if (textInput.isEmpty()) {
            System.out.println("Fichier d'entrée vide ou non trouvé");
            return;
        }

        System.out.println("Analyse des fréquences des caractères...");
        var symbList = new ArrayList<Symbol>();
        Map<Character, Integer> frequencyMap = new HashMap<>();
        for (char c : textInput.toCharArray()) {
            frequencyMap.merge(c, 1, Integer::sum);
        }
        frequencyMap.forEach((key, value) -> symbList.add(new Symbol(key, value)));

        // Compression
        System.out.println("Début de la compression...");
        writeIn(treeFileName, symbList);
        HashMap<Character, String> codes = huffmanMain(symbList);
        compressToFile(textInput, compressedFileName, codes);

        // Décompression
        System.out.println("Début de la décompression...");
        String decompressedText = decompressFromFile(compressedFileName, treeFileName);

        if (textInput.equals(decompressedText)) {
            System.out.println("Compression et décompression réussies !");

            File original = new File("Input/" + inputFileName);
            File compressed = new File("Compressed/" + compressedFileName);
            long originalSize = original.length();
            long compressedSize = compressed.length();
            double ratio = (1 - (double) compressedSize / originalSize) * 100;

            System.out.println("Statistiques :");
            System.out.printf("Taille originale: %d octets%n", originalSize);
            System.out.printf("Taille compressée: %d octets%n", compressedSize);
            System.out.printf("Taux de compression: %.2f%%%n", ratio);
            System.out.printf("Espace économisé: %d octets%n", originalSize - compressedSize);
        } else {
            System.out.println("Erreur: Le texte décompressé ne correspond pas à l'original");
        }
    }
}