grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}

EQUALS : '=';
SEMI : ';' ;
LCURLY : '{' ;
RCURLY : '}' ;
LSQUAREBRACK : '[' ;
RSQUAREBRACK : ']' ;
LPAREN : '(' ;
RPAREN : ')' ;
MUL : '*' ;
ADD : '+' ;
SUB : '-' ;
DIV : '/' ;
LESS : '<' ;
BIGGER : '>' ;
LESSOREQUAL : '<=';
BIGGEROREQUAL : '=>' ;
DOUBLEEQUAL: '==' ;
DIFFERENT: '!=' ;
ADDHIMSELF: '+=' ;
SUBHIMSELF: '-=' ;
MULTHIMSELF: '*=' ;
DIVHIMSELF: '/=' ;
INCREMENT: '++';
DECREMENT: '--';
OR : '||' ;
AND : '&&' ;
NEW : 'new' ;
EXCLAMATION : '!' ;
TRUE : 'true';
FALSE : 'false';
THIS : 'this' ;
STRING: 'String' ;

CLASS : 'class' ;
INT : 'int' ;
BOOLEAN : 'boolean' ;
PUBLIC : 'public' ;
RETURN : 'return' ;
IMPORT : 'import' ;
EXTEND : 'extends';
STATIC : 'static';
VOID : 'void';
IF : 'if' ;
ELSE: 'else';
WHILE: 'while';


INTEGER : [0] | ([1-9][0-9]*) ;
ID : [a-zA-Z_$][a-zA-Z0-9_$]* ;
SINGLELINECOMMENT: '//' .*? '\n' -> skip;
MULTILINECOMMENT: '/*' .*? '*/' -> skip;
WS : [ \t\n\r\f]+ -> skip ;

program
    : (importDecl)* classDecl EOF
    ;


importDecl
    : IMPORT name+=ID( '.' name+=ID)* SEMI #ImportStmt
    ;

classDecl
    : CLASS name=(ID|'main')
      (EXTEND extendname=(ID|'main'))?
      LCURLY
      (varDecl* methodDecl*)
      RCURLY
    ;

varDecl
    : type name=(ID|'length'|'main') SEMI #VarDeclaration
    ;

type locals[boolean isVararg=false, boolean isArray=false]
    : name= (INT|BOOLEAN|STRING|'main'|ID) ('['']'{$isArray=true;})? ('...'{$isVararg=true;})?
    | name= VOID
    ;

methodDecl locals[boolean isPublic=false, boolean isStatic=false,boolean isMain=false]
    : (PUBLIC {$isPublic=true;})?
      (STATIC {$isStatic=true;})?
        returnType=type name=ID
        LPAREN (param( ',' param)* )? RPAREN
        LCURLY varDecl* stmt* (RETURN expr SEMI)? RCURLY
    | (PUBLIC {$isPublic=true;})?
      (STATIC {$isStatic=true;})?
        returnType=type name='main' {$isMain=true;}
        LPAREN (param( ',' param)* )? RPAREN
        LCURLY varDecl* stmt* (RETURN expr SEMI)? RCURLY
     ;

param
    : type name=(ID|'main')
    ;

stmt
    : expr EQUALS expr SEMI #AssignStmt //
    | LSQUAREBRACK stmt* RSQUAREBRACK #BracketStmt
    | IF LPAREN expr RPAREN stmt ELSE stmt #IfElseStmt
    | LCURLY stmt* RCURLY #BracketStmt
    | WHILE LPAREN expr RPAREN stmt #WhileStmt
    | RETURN expr SEMI #ReturnStmt
    | expr SEMI #ExprStmt
    | expr LSQUAREBRACK expr RSQUAREBRACK EQUALS expr SEMI #ArrayAssignStmt
    ;

expr locals[boolean hasPoint=false]
    : LPAREN expr RPAREN #Parentesis
    | NEW INT LSQUAREBRACK expr RSQUAREBRACK #ArrayDeclaration
    | NEW name=(ID|'main') LPAREN RPAREN #NewClassDeclaration
    | name=THIS #Object
    | expr '.' value=(ID|'main') LPAREN (expr( ',' expr)* )? RPAREN {$hasPoint=true;} #FuncCall
    | value=(ID|'main') LPAREN (expr( ',' expr)* )? RPAREN #FuncCall
    | expr '.' 'length' #Length
    | expr LSQUAREBRACK expr RSQUAREBRACK #ArraySubscription
    | LSQUAREBRACK (expr (',' expr)* )? RSQUAREBRACK #ArrayExpr
    | EXCLAMATION expr #Negation
    | expr op=(MUL|DIV) expr #BinaryExpr //
    | expr op=(ADD|SUB) expr #BinaryExpr//
    | expr op=(LESS|BIGGER) expr #BinaryExpr
    //| expr op=(LESSOREQUAL|BIGGEROREQUAL|ADDHIMSELF|SUBHIMSELF|MULTHIMSELF|DIVHIMSELF|DOUBLEEQUAL|DIFFERENT) expr #BinaryOp
    | expr op=(AND|OR) expr #BinaryExpr
    | value=INTEGER #IntegerLiteral //
    | bool=(TRUE|FALSE) #Boolean
    | name=(ID|'length'|'main') #VarRefExpr //
    ;



