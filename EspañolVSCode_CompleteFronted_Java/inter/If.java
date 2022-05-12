package inter; 
import simbolos.*;
public class If extends Instr {
    Expr expr; Instr stmt;
    public If(Expr x, Instr s) {
        expr = x; stmt = s;
        if( expr.tipo != Tipo.Bool ) expr.error("se requiere booleano en if");
    }
    public void gen(int b, int a) {
        int etiqueta = nuevaEtiqueta(); 
        expr.salto(0, a); 
        emitirEtiqueta(etiqueta); stmt.gen(etiqueta, a);
    }
}