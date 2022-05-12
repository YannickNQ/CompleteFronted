package inter;
import analizadorLexico.*;
public class Nodo {
    int linelex = 0;
    Nodo() { linelex = AnalizadorLexico.linea; }
    void error(String s) { throw new Error("cerca de la linea "+linelex+": "+s); }
    static int etiquetas = 0;
    public int nuevaEtiqueta() { return ++etiquetas; }
    public void emitirEtiqueta(int i) { System.out.print("L" + i + ":"); }
    public void emitir(String s) { System.out.println("\t" + s); }
}