package analizador; 
import java.io.*; import analizadorLexico.*; import simbolos.*; import inter.*;
public class Analizador {
    private AnalizadorLexico lex; 
    private Token busca; 
    Ent sup = null; 
    int usado = 0; 
    public Analizador(AnalizadorLexico l) throws IOException { lex = l; mover(); }
    void mover() throws IOException { busca = lex.explorar(); }
    void error(String s) { throw new Error("near linea "+lex.linea+": "+s); }
    void coincidir(int t) throws IOException {
        if( busca.etiqueta == t ) mover();
        else error("error de sintaxis");
    }
    public void programa() throws IOException { 
        Instr s = bloque();
        int inicio = s.nuevaEtiqueta(); int despues = s.nuevaEtiqueta();
        s.emitirEtiqueta(inicio); s.gen(inicio, despues); s.emitirEtiqueta(despues);
    }
    Instr bloque() throws IOException { 
        coincidir('{'); Ent entGuardado = sup; sup = new Ent(sup);
        decls(); Instr s = instrs();
        coincidir('}'); sup = entGuardado;
        return s;
    }
    void decls() throws IOException {
        while( busca.etiqueta == Etiqueta.BASIC ) { 
            Tipo p = tipo(); Token tok = busca; coincidir(Etiqueta.ID); coincidir(';');
            Id id = new Id((Palabra)tok, p, usado);
            sup.put( tok, id );
            usado = usado + p.anchura;
        }
    }
    Tipo tipo() throws IOException {
        Tipo p = (Tipo)busca;
        coincidir(Etiqueta.BASIC);
        if( busca.etiqueta != '[' ) return p; 
        else return dims(p); 
    }
    Tipo dims(Tipo p) throws IOException {
        coincidir('['); Token tok = busca; coincidir(Etiqueta.NUM); coincidir(']');
        if( busca.etiqueta == '[' )
        p = dims(p);
        return new Arreglo(((Num)tok).valor, p);
    }
    Instr instrs() throws IOException {
        if ( busca.etiqueta == '}' ) return Instr.Null;
        else return new Sec(instr(), instrs());
    }
    Instr instr() throws IOException {
        Expr x; Instr s, s1, s2;
        Instr instrGuardada; 
        switch( busca.etiqueta ) {
        case ';':
            mover();
            return Instr.Null;
        case Etiqueta.IF:
            coincidir(Etiqueta.IF); coincidir('('); x = bool(); coincidir(')');
            s1 = instr();
            if( busca.etiqueta != Etiqueta.ELSE ) return new If(x, s1);
            coincidir(Etiqueta.ELSE);
            s2 = instr();
            return new Else(x, s1, s2);
        case Etiqueta.WHILE:
            While whilenode = new While();
            instrGuardada = Instr.Circundante; Instr.Circundante = whilenode;
            coincidir(Etiqueta.WHILE); coincidir('('); x = bool(); coincidir(')');
            s1 = instr();
            whilenode.init(x, s1);
            Instr.Circundante = instrGuardada; 
            return whilenode;
        case Etiqueta.DO:
            Do donode = new Do();
            instrGuardada = Instr.Circundante; Instr.Circundante = donode;
            coincidir(Etiqueta.DO);
            s1 = instr();
            coincidir(Etiqueta.WHILE); coincidir('('); x = bool(); coincidir(')'); coincidir(';');
            donode.init(s1, x);
            Instr.Circundante = instrGuardada; 
            return donode;
        case Etiqueta.FOR:
            For fornode = new For();
            instrGuardada = Instr.Circundante; Instr.Circundante = fornode;
            coincidir(Etiqueta.FOR); coincidir('(');s = instr();  x=bool(); coincidir(';'); s1 = instr(); coincidir(')');
            s2 = instr();
            fornode.init(s, x, s1, s2);
            Instr.Circundante = instrGuardada;
            return fornode;
        case Etiqueta.BREAK:
            coincidir(Etiqueta.BREAK); coincidir(';');
            return new Break();
        case '{':
            return bloque();
        default:
            return asignar();
        }
    }
    Instr asignar() throws IOException {
        Instr instr; Token t = busca;
        coincidir(Etiqueta.ID);
        Id id = sup.get(t);
        if( id == null ) error(t.toString() + " undeclared");
        if( busca.etiqueta == '=' ) { 
            mover(); instr = new Est(id, bool());
        }
        else { 
            Acceso x = desplazamiento(id);
            coincidir('='); instr = new EstElem(x, bool());
        }
        coincidir(';');
        return instr;
    }
    Expr bool() throws IOException {
        Expr x = join();
        while( busca.etiqueta == Etiqueta.OR ) {
            Token tok = busca; mover(); x = new Or(tok, x, join());
        }
        return x;
    }
    Expr join() throws IOException {
        Expr x = equality();
        while( busca.etiqueta == Etiqueta.AND ) {
            Token tok = busca; mover(); x = new And(tok, x, equality());
        }
        return x;
    }
    Expr equality() throws IOException {
        Expr x = rel();
        while( busca.etiqueta == Etiqueta.EQ || busca.etiqueta == Etiqueta.NE ) {
            Token tok = busca; mover(); x = new Rel(tok, x, rel());
        }
        return x;
    }
    Expr rel() throws IOException {
        Expr x = expr();
        switch( busca.etiqueta ) {
        case '<': case Etiqueta.LE: case Etiqueta.GE: case '>':
            Token tok = busca; mover(); return new Rel(tok, x, expr());
        default:
            return x;
        }
    }
    Expr expr() throws IOException {
        Expr x = term();
        while( busca.etiqueta == '+' || busca.etiqueta == '-' ) {
            Token tok = busca; mover(); x = new Arit(tok, x, term());
        }
        return x;
    }
    Expr term() throws IOException {
        Expr x = unary();
        while(busca.etiqueta == '*' || busca.etiqueta == '/' ) {
            Token tok = busca; mover(); x = new Arit(tok, x, unary());
        }
        return x;
    }
    Expr unary() throws IOException {
        if( busca.etiqueta == '-' ) {
            mover(); return new Unario(Palabra.minus, unary());
        }
        else if( busca.etiqueta == '!' ) {
            Token tok = busca; mover(); return new Not(tok, unary());
        }
        else return factor();
    }
    Expr factor() throws IOException {
        Expr x = null;
        switch( busca.etiqueta ) {
        case '(':
            mover(); x = bool(); coincidir(')');
            return x;
        case Etiqueta.NUM:
            x = new Constante(busca, Tipo.Int); mover(); return x;
        case Etiqueta.REAL:
            x = new Constante(busca, Tipo.Float); mover(); return x;
        case Etiqueta.TRUE:
            x = Constante.True; mover(); return x;
        case Etiqueta.FALSE:
            x = Constante.False; mover(); return x;
        default:
            error("syntax error");
            return x;
        case Etiqueta.ID:
            String s = busca.toString();
            Id id = sup.get(busca);
            if( id == null ) error(busca.toString() + " undeclared");
            mover();
            if( busca.etiqueta != '[' ) return id;
            else return desplazamiento(id);
        }
    }
    Acceso desplazamiento(Id a) throws IOException { 
        Expr i; Expr w; Expr t1, t2; Expr ubic; 
        Tipo tipo = a.tipo;
        coincidir('['); i = bool(); coincidir(']'); 
        tipo = ((Arreglo)tipo).de;
        w = new Constante(tipo.anchura);
        t1 = new Arit(new Token('*'), i, w);
        ubic = t1;
        while( busca.etiqueta == '[' ) { 
            coincidir('['); i = bool(); coincidir(']');
            tipo = ((Arreglo)tipo).de;
            w = new Constante(tipo.anchura);   
            t1 = new Arit(new Token('*'), i, w);
            t2 = new Arit(new Token('+'), ubic, t1);
            ubic = t2;
        }
        return new Acceso(a, ubic, tipo);
    }
}