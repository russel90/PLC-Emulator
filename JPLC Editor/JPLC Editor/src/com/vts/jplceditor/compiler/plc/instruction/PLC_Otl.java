/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vts.jplceditor.compiler.plc.instruction;

import com.vts.jplceditor.compiler.pic18.BitInstruction;
import com.vts.jplceditor.compiler.pic18.ControlInstruction;
import com.vts.jplceditor.compiler.pic18.LiteralInstruction;
import com.vts.jplceditor.compiler.pic18.Opcode;
import com.vts.jplceditor.compiler.pic18.Pic18Instruction;
import com.vts.jplceditor.compiler.pic18.Pic18Mem;
import com.vts.jplceditor.compiler.plc.PLCOperand;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Vusman
 */
public class PLC_Otl extends Pic18Mem implements PLCInstr {

    private final List<Pic18Instruction> asm;
    private final int bitAdress;
    private int org;
    private final byte EN = 0;
    private final byte ENO = 1;
    private int wordCount;
    private int count;
    private final PLCOperand prevENO;

    public PLC_Otl(int org, List<PLCOperand> parameters, PLCOperand prevENO) {

        this.bitAdress = parameters.get(0).getAddress().getFileAddr();
        asm = new ArrayList<>();
        this.org = org;
        this.prevENO = prevENO;
        PLCOperand in = parameters.get(2);
        checkENO();
        otl(in);
    }

    private void checkENO() {
        int code_start, clearENO;

        //First set the EN depending on the prev ENO
        if (prevENO == null) { //If no prev ENO, this is rung start
            banksel(bitAdress);
            asm.add(new BitInstruction(Opcode.BSF, bitAdress, EN, bsr));
            wordCount = 2;
        } else {   // Else set this EN depending on the prev ENO
            code_start = org + 18;
            clearENO = code_start + 8;  //clear_ENO in instruction code
            int clrEN = org + 12;
            int enFile = prevENO.getAddress().getFileAddr();
            /*0*/ banksel(enFile);
            /*2*/ asm.add(new BitInstruction(Opcode.BTFSS, enFile, ENO, bsr)); // If ENO set, set EN
            /*4*/ branch(org + 4, clrEN);
            /*6*/ banksel(bitAdress);
            /*8*/ asm.add(new BitInstruction(Opcode.BSF, bitAdress, EN, bsr));
            /*10*/ branch(org + 10, code_start);


            //clrEN address
            /*12*/ banksel(bitAdress);
            /*14*/ asm.add(new BitInstruction(Opcode.BCF, bitAdress, EN, bsr));
            /*16*/ branch(org + 16, clearENO);
            wordCount = 9;
        }
        org += (wordCount * 2);
    }

    private void otl(PLCOperand in) {
        int code_start;

        int endif;
        byte bit = in.getAddress().getBitNo();
        int input = in.getAddress().getFileAddr();


        //IF EN
        //ENO = TRUE
        //BIT = FALSE
        //ENDIF       
        /*0*/ banksel(bitAdress);
        /*2*/ asm.add(new BitInstruction(Opcode.BSF, bitAdress, ENO, bsr));
        /*4*/ banksel(input);
        /*6*/ asm.add(new BitInstruction(Opcode.BSF, input, bit, bsr));
        wordCount += 4;
        if (prevENO != null) {
            //clearENO address
            /*8*/ banksel(bitAdress);
            /*10*/ asm.add(new BitInstruction(Opcode.BCF, bitAdress, ENO, bsr));
            wordCount+=2;
        }
        
    }

    @Override
    public List<Pic18Instruction> getPicAsm() {
        return asm;
    }

    @Override
    public int getwordCount() {
        return wordCount;
    }

    private void banksel(int file) {
        byte bank = (byte) (file >> 8);
        asm.add(new LiteralInstruction(Opcode.MOVLB, bank));
    }

    private void branch(int currentAddress, int targetAddress) {
        //address = PC + 2 + 2n
        int branchAddress = targetAddress - currentAddress - 2;
        branchAddress /= 2;
        asm.add(new ControlInstruction(Opcode.BRA, branchAddress));
    }
}
