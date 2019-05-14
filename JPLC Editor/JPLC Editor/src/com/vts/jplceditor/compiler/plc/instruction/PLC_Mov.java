/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vts.jplceditor.compiler.plc.instruction;

import com.vts.jplceditor.compiler.pic18.BitInstruction;
import com.vts.jplceditor.compiler.pic18.ByteInstruction;
import com.vts.jplceditor.compiler.pic18.ControlInstruction;
import com.vts.jplceditor.compiler.pic18.LiteralInstruction;
import com.vts.jplceditor.compiler.pic18.Opcode;
import com.vts.jplceditor.compiler.pic18.Pic18Instruction;
import com.vts.jplceditor.compiler.pic18.Pic18Mem;
import com.vts.jplceditor.compiler.plc.PLCDataType;
import com.vts.jplceditor.compiler.plc.PLCOperand;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Vusman
 */
public class PLC_Mov extends Pic18Mem implements PLCInstr {

    private final List<Pic18Instruction> asm;
    private final int bitAdress;
    private int org;
    private final byte EN = 0;
    private final byte ENO = 1;
    private int wordCount;
    private int count;
    private final PLCOperand prevENO, dest;
    private final int moveByteSize = 10;
    private final int moveIntSize = 18;

    public PLC_Mov(int org, List<PLCOperand> parameters, PLCOperand prevENO) {

        this.bitAdress = parameters.get(0).getAddress().getFileAddr();
        asm = new ArrayList<>();
        this.org = org;
        PLCOperand in1, in2;
        in1 = parameters.get(2);
        in2 = parameters.get(3);
        dest = parameters.get(4);
        this.prevENO = prevENO;

        if (in1.getDataType() == PLCDataType.BYTE) {
            checkENO(moveByteSize);
            moveByte(in1, in2);
        } else if (in1.getDataType() == PLCDataType.INT) {
            checkENO(moveIntSize);
            moveInt(in1, in2);
        } else if (in1.getDataType() == PLCDataType.INT) {
            checkENO(moveIntSize);
            moveLiteral(in1, in2);
        }
    }

    private void checkENO(int endif) {
        int code_start, clearENO;

        //First set the EN depending on the prev ENO
        if (prevENO == null) { //If no prev ENO, this is rung start
            banksel(bitAdress);
            asm.add(new BitInstruction(Opcode.BSF, bitAdress, EN, bsr));
            wordCount = 2;
        } else {   // Else set this EN depending on the prev ENO
            code_start = org + 18;
            clearENO = code_start + endif;  //clear_ENO in instruction code
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

    private void moveByte(PLCOperand in1, PLCOperand in2) {
        int source, dest;
        source = in1.getAddress().getFileAddr();
        dest = in2.getAddress().getFileAddr();


        //ENO = TRUE
        //IF EN
        //DEST = SOURCE
        //ENDIF
        /*0*/ asm.add(new BitInstruction(Opcode.BSF, bitAdress, ENO, bsr));
        /*2*/ banksel(source);
        /*4*/ asm.add(new ByteInstruction(Opcode.MOVF, source, bsr, w));
        /*6*/ banksel(dest);
        /*8*/ asm.add(new ByteInstruction(Opcode.MOVWF, dest, bsr, false));

        //endif address
        /*10*/ banksel(bitAdress);
        /*12*/ asm.add(new BitInstruction(Opcode.BCF, bitAdress, ENO, bsr));

        wordCount += 7;
    }

    private void moveInt(PLCOperand in1, PLCOperand in2) {
        int source, dest;
        source = in1.getAddress().getFileAddr();
        dest = in2.getAddress().getFileAddr();


        //ENO = TRUE
        //IF EN
        //DEST = SOURCE
        //ENDIF
        /*0*/ asm.add(new BitInstruction(Opcode.BSF, bitAdress, ENO, bsr));
        /*2*/ banksel(source);
        /*4*/ asm.add(new ByteInstruction(Opcode.MOVF, source, bsr, w));
        /*6*/ banksel(dest);
        /*8*/ asm.add(new ByteInstruction(Opcode.MOVWF, dest, bsr, false));
        /*10*/ banksel(source + 1);
        /*12*/ asm.add(new ByteInstruction(Opcode.MOVF, source + 1, bsr, w));
        /*14*/ banksel(dest + 1);
        /*16*/ asm.add(new ByteInstruction(Opcode.MOVWF, dest + 1, bsr, false));


        //endif address
        /*18*/ banksel(bitAdress);
        /*20*/ asm.add(new BitInstruction(Opcode.BCF, bitAdress, ENO, bsr));

        wordCount += 11;
    }

    private void moveLiteral(PLCOperand in1, PLCOperand in2) {
        int source, dest;
        source = in1.getAddress().getFileAddr();
        dest = in2.getAddress().getFileAddr();


        //ENO = TRUE
        //IF EN
        //DEST = SOURCE
        //ENDIF
        /*0*/ asm.add(new BitInstruction(Opcode.BSF, bitAdress, ENO, bsr));
        /*2*/ banksel(source);
        /*4*/ asm.add(new ByteInstruction(Opcode.MOVF, source, bsr, w));
        /*6*/ banksel(dest);
        /*8*/ asm.add(new LiteralInstruction(Opcode.MOVLW, 0xFF & source));
        /*10*/ banksel(source + 1);
        /*12*/ asm.add(new LiteralInstruction(Opcode.MOVLW, (source & 0xFF00) >> 8));
        /*14*/ banksel(dest + 1);
        /*16*/ asm.add(new ByteInstruction(Opcode.MOVWF, dest + 1, bsr, false));


        //endif address
        /*18*/ banksel(bitAdress);
        /*20*/ asm.add(new BitInstruction(Opcode.BCF, bitAdress, ENO, bsr));

        wordCount += 11;
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
