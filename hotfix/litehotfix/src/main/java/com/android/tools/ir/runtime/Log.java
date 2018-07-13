package com.android.tools.ir.runtime;

import java.util.logging.Level;

public class Log {

    public static Logg logging=new Logg(){
        @Override
        public void log(Object... args) {

        }

        @Override
        public boolean isLoggable(Level fine) {
            return false;
        }
    };

    public static interface Logg{
        void log(Object...args);

        boolean isLoggable(Level fine);
    }
}
