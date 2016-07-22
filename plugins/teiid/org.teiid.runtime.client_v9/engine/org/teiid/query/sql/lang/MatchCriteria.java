/* Generated By:JJTree: Do not edit this line. MatchCriteria.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=true,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package org.teiid.query.sql.lang;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.teiid.core.util.LRUCache;
import org.teiid.core.util.PropertiesUtils;
import org.teiid.designer.query.sql.lang.IMatchCriteria;
import org.teiid.query.parser.LanguageVisitor;
import org.teiid.query.parser.TeiidParser;
import org.teiid.query.sql.symbol.Expression;
import org.teiid.runtime.client.Messages;
import org.teiid.runtime.client.TeiidClientException;

/**
 *
 */
public class MatchCriteria extends Criteria
    implements PredicateCriteria, IMatchCriteria<Expression, LanguageVisitor> {

    /** The internal null escape character */
    public static final char NULL_ESCAPE_CHAR = 0;

    private static final char DEFAULT_ESCAPE_CHAR = PropertiesUtils.getBooleanProperty(System.getProperties(), "org.teiid.backslashDefaultMatchEscape", false)?'\\':NULL_ESCAPE_CHAR; //$NON-NLS-1$

    /** The left-hand expression. */
    private Expression leftExpression;
    
    /** The right-hand expression. */
    private Expression rightExpression;

    private boolean negated;

    /** The escape character or '' if there is none */
    private char escapeChar = DEFAULT_ESCAPE_CHAR;

    private MatchMode mode = MatchMode.LIKE;

    /**
     * @param p
     * @param id
     */
    public MatchCriteria(TeiidParser p, int id) {
        super(p, id);
    }

    /**
     * Get left expression.
     * @return Left expression
     */
    @Override
    public Expression getLeftExpression() {
        return this.leftExpression;
    }

    /**
     * Set left expression.
     * @param expression expression
     */
    @Override
    public void setLeftExpression(Expression expression) { 
        this.leftExpression = expression;
    }

    /**
     * Get right expression.
     * @return right expression
     */
    @Override
    public Expression getRightExpression() {
        return this.rightExpression;
    }

    /**
     * Set right expression.
     * @param expression expression
     */
    @Override
    public void setRightExpression(Expression expression) { 
        this.rightExpression = expression;
    }

    /**
     * Returns whether this criteria is negated.
     * @return flag indicating whether this criteria contains a NOT
     */
    @Override
    public boolean isNegated() {
        return negated;
    }
    
    /**
     * Sets the negation flag for this criteria.
     * @param negationFlag true if this criteria contains a NOT; false otherwise
     */
    @Override
    public void setNegated(boolean negationFlag) {
        negated = negationFlag;
    }

    /**
     * Get the escape character, which can be placed before the wildcard or single match
     * character in the expression to prevent it from being used as a wildcard or single
     * match.  The escape character must not be used elsewhere in the expression.
     * For example, to match "35%" without activating % as a wildcard, set the escape character
     * to '$' and use the expression "35$%".     
     * @return Escape character, if not set will return {@link #NULL_ESCAPE_CHAR}
     */
    @Override
    public char getEscapeChar() {
        return this.escapeChar;
    }

    /**
     * Set the escape character which can be used when the wildcard or single
     * character should be used literally.
     * @param escapeChar New escape character
     */
    @Override
    public void setEscapeChar(char escapeChar) {
        this.escapeChar = escapeChar;
    }

    @Override
    public MatchMode getMode() {
        return mode;
    }

    /**
     * @param mode
     */
    public void setMode(MatchMode mode) {
        this.mode = mode;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + this.escapeChar;
        result = prime * result + ((this.leftExpression == null) ? 0 : this.leftExpression.hashCode());
        result = prime * result + ((this.mode == null) ? 0 : this.mode.hashCode());
        result = prime * result + (this.negated ? 1231 : 1237);
        result = prime * result + ((this.rightExpression == null) ? 0 : this.rightExpression.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        MatchCriteria other = (MatchCriteria)obj;
        if (this.escapeChar != other.escapeChar) return false;
        if (this.leftExpression == null) {
            if (other.leftExpression != null) return false;
        } else if (!this.leftExpression.equals(other.leftExpression)) return false;
        if (this.mode != other.mode) return false;
        if (this.negated != other.negated) return false;
        if (this.rightExpression == null) {
            if (other.rightExpression != null) return false;
        } else if (!this.rightExpression.equals(other.rightExpression)) return false;
        return true;
    }

    /** Accept the visitor. **/
    @Override
    public void acceptVisitor(LanguageVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public MatchCriteria clone() {
        MatchCriteria clone = new MatchCriteria(this.parser, this.id);

        if(getRightExpression() != null)
            clone.setRightExpression(getRightExpression().clone());
        if(getLeftExpression() != null)
            clone.setLeftExpression(getLeftExpression().clone());
        if(getMode() != null)
            clone.setMode(getMode());
        clone.setNegated(isNegated());
        clone.setEscapeChar(getEscapeChar());

        return clone;
    }

    private final static LRUCache<List<?>, Pattern> patternCache = new LRUCache<List<?>, Pattern>(100);
    
    /**
     * @param newPattern
     * @param originalPattern
     * @param flags
     * @return patter object based on the given parameters
     * @throws Exception
     */
    public static Pattern getPattern(String newPattern, String originalPattern, int flags) throws Exception {
        List<?> key = Arrays.asList(newPattern, flags);
        Pattern p = patternCache.get(key);
        if (p == null) {
            try {           
                p = Pattern.compile(newPattern, Pattern.DOTALL);
                patternCache.put(key, p);
            } catch(PatternSyntaxException e) {
                 throw new TeiidClientException(e, Messages.gs(Messages.TEIID.TEIID30448, new Object[]{originalPattern, e.getMessage()}));
            }
        }
        return p;
    }

    /**
     * <p>Utility to convert the pattern into a different match syntax</p>
     */
    public static class PatternTranslator {
        private char[] reserved;
        private char newEscape;
        private char[] toReplace;
        private String[] replacements;
        private int flags;
        private final LRUCache<List<?>, Pattern> cache = new LRUCache<List<?>, Pattern>(100);

        /**
         * @param toReplace replacement for %
         * @param replacements replacement for _
         * @param reserved sorted array of reserved chars in the new match syntax
         * @param newEscape escape char in the new syntax
         * @param flags extra bitwise flags
         */
        public PatternTranslator(char[] toReplace, String[] replacements, char[] reserved, char newEscape, int flags) {
            this.reserved = reserved;
            this.newEscape = newEscape;
            this.toReplace = toReplace;
            this.replacements = replacements;
            this.flags = flags;
        }
        
        /**
         * @param pattern
         * @param escape
         * @return translated pattern
         * @throws Exception
         */
        public Pattern translate(String pattern, char escape) throws Exception {
            List<?> key = Arrays.asList(pattern, escape);
            Pattern result = null;
            synchronized (cache) {
                result = cache.get(key);
            }
            if (result == null) {
                String newPattern = getPatternString(pattern, escape);
                result = getPattern(newPattern, pattern, flags);
                synchronized (cache) {
                    cache.put(key, result);
                }
            }
            return result;
        }

        /**
         * @param pattern
         * @param escape
         * @return pattern string based on given pattern
         * @throws Exception
         */
        public String getPatternString(String pattern, char escape)
                throws Exception {
            int startChar = 0;
            StringBuffer newPattern = new StringBuffer(pattern.length());
            if (pattern.length() > 0 && pattern.charAt(0) == '%') {
                startChar = 1;
            } else {
                newPattern.append('^');
            }
            
            boolean escaped = false;
            boolean endsWithMatchAny = false;
            for (int i = startChar; i < pattern.length(); i++) {
                char character = pattern.charAt(i);
                
                if (character == escape && character != NULL_ESCAPE_CHAR) {
                    if (escaped) {
                        appendCharacter(newPattern, character);
                        escaped = false;
                    } else {
                        escaped = true;
                    }
                } else {
                    int index = Arrays.binarySearch(toReplace, character);
                    if (index >= 0) {
                        if (escaped) {
                            appendCharacter(newPattern, character);
                            escaped = false;
                        } else {
                            if (character == '%' && i == pattern.length() - 1) {
                                endsWithMatchAny = true;
                                continue;
                            }
                            newPattern.append(replacements[index]);
                        }
                    } else {
                        if (escaped) {
                             throw new TeiidClientException(Messages.gs(Messages.TEIID.TEIID30449, new Object[] {pattern, new Character(escape)}));
                        }
                        appendCharacter(newPattern, character);
                    }
                }
            }
            
            if (escaped) {
                 throw new TeiidClientException(Messages.gs(Messages.TEIID.TEIID30449, new Object[] {pattern, new Character(escape)}));
            }
            
            if (!endsWithMatchAny) {
                newPattern.append('$');
            }
            return newPattern.toString();
        }
        
        private void appendCharacter(StringBuffer newPattern, char character) {
            if (Arrays.binarySearch(this.reserved, character) >= 0) {
                newPattern.append(this.newEscape);
            } 
            newPattern.append(character);
        }
        
    }
}
/* JavaCC - OriginalChecksum=0f89c892141b9a7e6acaf4cfc0d222f5 (do not edit this line) */
