package huffman.tree;

import java.util.HashMap;

public class Tree extends Node{
    private final Node lChild;
    private final Node rChild;
    private final int frequency;

    public Tree(Node lChild, Node rChild) {
        this.lChild = lChild;
        this.rChild = rChild;
        this.frequency = lChild.getFrequency()+rChild.getFrequency();
    }

    @Override
    public int getFrequency() {
        return this.frequency;
    }

    @Override
    void doTableCreation(HashMap<Character, String> table, String prefix) {
        lChild.doTableCreation(table, prefix+"0");
        rChild.doTableCreation(table, prefix+"1");
    }

    public HashMap<Character, String> createTable(){
        HashMap<Character, String> dico = new HashMap<>();
        doTableCreation(dico, "");
        return dico;
    }

    public Node getlChild() {
        return lChild;
    }

    public Node getrChild() {
        return rChild;
    }
}
