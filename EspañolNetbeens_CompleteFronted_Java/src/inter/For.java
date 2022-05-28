package inter;
import simbolos.*;
public class For extends Instr {
    Expr expr; Instr instr1, instr2, instr3;
    public For(){ expr = null; instr1 = null; instr2 = null; instr3 = null;}
    public void init(Instr s1, Expr x, Instr s2, Instr s3){
        expr = x; instr1 = s1; instr2 = s2; instr3 = s3;
        if(expr.tipo != Tipo.Bool){
            expr.error("Se requiere booleano en for");
        }
    }
    public void gen(int b, int a){
        despues = a; 
        int etiqueta = nuevaEtiqueta();//para instr1
        int etiqueta2 = nuevaEtiqueta();// para instr3
        int etiqueta3 = nuevaEtiqueta(); //para instr2
        instr1.gen(b, a);
        emitirEtiqueta(etiqueta);
        expr.salto(0, a);
        emitirEtiqueta(etiqueta2);
        instr3.gen(etiqueta, a);
        emitirEtiqueta(etiqueta3);
        instr2.gen(etiqueta2, a);
        emitir("goto L" + etiqueta);
    }
}
