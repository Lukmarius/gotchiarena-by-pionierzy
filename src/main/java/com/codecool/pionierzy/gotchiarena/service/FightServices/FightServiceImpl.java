package com.codecool.pionierzy.gotchiarena.service.FightServices;

import com.codecool.pionierzy.gotchiarena.model.*;
import com.codecool.pionierzy.gotchiarena.service.GotchiServices.GotchiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class FightServiceImpl implements FightService {
    private final GotchiService gotchiService;
    private HashMap<Room, RoundMessage> roomRoundMessageMap = new HashMap<>();
    private HashMap<AttackType, ArrayList<AttackType>> strongAgainst;
    private HashMap<AttackType, ArrayList<AttackType>> weakAgainst;
    private HashMap<RoundAction, Double> actionModifier;
    private HashMap<RoundAction, Double> defenceModifier;

    @Autowired
    public FightServiceImpl(GotchiService gotchiService) {
        this.gotchiService = gotchiService;

        actionModifier = new HashMap<>();
        actionModifier.put(RoundAction.PRIMARY_ATTACK, 1.0);
        actionModifier.put(RoundAction.SECONDARY_ATTACK, 0.9);
        actionModifier.put(RoundAction.EVADE, 0.9);
        actionModifier.put(RoundAction.DEFEND, 0.0);

        defenceModifier = new HashMap<>();
        defenceModifier.put(RoundAction.DEFEND, 1.5);
        defenceModifier.put(RoundAction.EVADE, 1.0);
        defenceModifier.put(RoundAction.PRIMARY_ATTACK, 1.0);
        defenceModifier.put(RoundAction.SECONDARY_ATTACK, 1.0);

        strongAgainst = new HashMap<>();
        for (AttackType type : AttackType.values()){
            strongAgainst.put(type, new ArrayList<AttackType>());
        }
        strongAgainst.get(AttackType.FIRE).add(AttackType.PLANT);
        strongAgainst.get(AttackType.FIRE).add(AttackType.ICE);
        strongAgainst.get(AttackType.FIRE).add(AttackType.MAGIC);

        strongAgainst.get(AttackType.WATER).add(AttackType.FIRE);
        strongAgainst.get(AttackType.WATER).add(AttackType.GROUND);

        strongAgainst.get(AttackType.PLANT).add(AttackType.WATER);
        strongAgainst.get(AttackType.PLANT).add(AttackType.GROUND);

        strongAgainst.get(AttackType.ELECTRIC).add(AttackType.WATER);
        strongAgainst.get(AttackType.ELECTRIC).add(AttackType.PLANT);

        strongAgainst.get(AttackType.ICE).add(AttackType.WATER);
        strongAgainst.get(AttackType.ICE).add(AttackType.ELECTRIC);
        strongAgainst.get(AttackType.ICE).add(AttackType.PLANT);

        strongAgainst.get(AttackType.GROUND).add(AttackType.FIRE);
        strongAgainst.get(AttackType.GROUND).add(AttackType.ELECTRIC);

        strongAgainst.get(AttackType.MAGIC).add(AttackType.NORMAL);


        weakAgainst = new HashMap<>();
        for (AttackType type : AttackType.values()){
            weakAgainst.put(type, new ArrayList<AttackType>());
        }
        weakAgainst.get(AttackType.FIRE).add(AttackType.WATER);
        weakAgainst.get(AttackType.FIRE).add(AttackType.GROUND);

        weakAgainst.get(AttackType.WATER).add(AttackType.PLANT);

        weakAgainst.get(AttackType.PLANT).add(AttackType.FIRE);

        weakAgainst.get(AttackType.ELECTRIC).add(AttackType.GROUND);

        weakAgainst.get(AttackType.ICE).add(AttackType.FIRE);
        weakAgainst.get(AttackType.ICE).add(AttackType.GROUND);

        weakAgainst.get(AttackType.GROUND).add(AttackType.ICE);
        weakAgainst.get(AttackType.GROUND).add(AttackType.MAGIC);

        weakAgainst.get(AttackType.NORMAL).add(AttackType.MAGIC);
    }

    @Override
    public boolean receiveAction(Room room, User user, RoundAction action) {
        RoundMessage roundMessage = this.roomRoundMessageMap.get(room);
        if (user.equals(room.getOwner())) {
            roundMessage.setOwnerAction(action, room.getOwnerGotchi());
            System.out.println("set owner action");
            System.out.println(roundMessage.getOwnerAction());
        }
        else {
            roundMessage.setOpponentAction(action, room.getOpponentGotchi());
            System.out.println("set opp action");
            System.out.println(roundMessage.getOpponentAction());
        }
        if (roundMessage.getOwnerAction() != null && roundMessage.getOpponentAction() != null) {
            resolveRound(room, roundMessage);
            System.out.println("BOTH");
            return true;
        }
        System.out.println("ONE");
        return false;
    }

    @Override
    public void resolveRound(Room room, RoundMessage roundMessage) {
        RoundAction ownerAction = roundMessage.getOwnerAction();
        RoundAction opponentAction = roundMessage.getOpponentAction();

        Gotchi ownGotchi = room.getOwnerGotchi();
        Gotchi oppGotchi = room.getOpponentGotchi();

        double hit;
        double defence;
        double defModifier;
        int ownerHpLoss = 0;
        int oppHpLoss = 0;

        //Attacking or defending:
        if (ownerAction != RoundAction.EVADE && opponentAction != RoundAction.EVADE) {
            //owner attack:
            hit = ownGotchi.getAttack() * actionModifier.get(ownerAction) *
                    strongOrWeakModifier(ownerAction, ownGotchi, oppGotchi.getType());
            defence = oppGotchi.getDefence() * defenceModifier.get(opponentAction);
            oppHpLoss = calculateHpLoss(hit, defence);

            //opponentAttack:
            hit = oppGotchi.getAttack() * actionModifier.get(opponentAction) *
                    strongOrWeakModifier(opponentAction, oppGotchi, ownGotchi.getType());
            defence = ownGotchi.getDefence() * defenceModifier.get(ownerAction);
            ownerHpLoss = calculateHpLoss(hit, defence);
            System.out.println("attacking or defending");
        }

        //one evading:
        else if (ownerAction == RoundAction.EVADE ^ opponentAction == RoundAction.EVADE){
            if (ownerAction == RoundAction.EVADE){
                if (succesOfEvade(ownGotchi.getSpeed(), oppGotchi.getSpeed())){
                    RoundAction randomAction = chooseRandomAction();
                    hit = ownGotchi.getAttack() * actionModifier.get(randomAction) *
                            strongOrWeakModifier(randomAction, ownGotchi, oppGotchi.getType());
                    defence = oppGotchi.getDefence() * oppGotchi.getSpeed()/200;
                    oppHpLoss = calculateHpLoss(hit, defence);
                    System.out.println("succes of owner evade");
                }
                else {
                    hit = oppGotchi.getAttack() * actionModifier.get(opponentAction) *
                            strongOrWeakModifier(opponentAction, oppGotchi, ownGotchi.getType());
                    defence = ownGotchi.getDefence() * defenceModifier.get(ownerAction);
                    ownerHpLoss = calculateHpLoss(hit, defence);
                    System.out.println("failure of owner evade");
                }
            }
            else {
                if (succesOfEvade(oppGotchi.getSpeed(), ownGotchi.getSpeed())){
                    RoundAction randomAction = chooseRandomAction();
                    hit = oppGotchi.getAttack() * actionModifier.get(randomAction) *
                            strongOrWeakModifier(randomAction, oppGotchi, ownGotchi.getType());
                    defence = ownGotchi.getDefence() * ownGotchi.getSpeed()/200;
                    ownerHpLoss = calculateHpLoss(hit, defence);
                    System.out.println("succes of opp evade");
                }
                else {
                    hit = ownGotchi.getAttack() * actionModifier.get(ownerAction) *
                            strongOrWeakModifier(ownerAction, ownGotchi, oppGotchi.getType());
                    defence = oppGotchi.getDefence() * defenceModifier.get(opponentAction);
                    oppHpLoss = calculateHpLoss(hit, defence);
                    System.out.println("failure of opp evade");
                }
            }
        }
        System.out.println("defending or evading both");

        roundMessage.setOwnerHPLoss(ownerHpLoss);
        roundMessage.setOpponentHPLoss(oppHpLoss);

        ownGotchi.setHealth(ownGotchi.getHealth() - ownerHpLoss);
        gotchiService.save(ownGotchi);

        oppGotchi.setHealth(oppGotchi.getHealth() - oppHpLoss);
        gotchiService.save(oppGotchi);

    }

    private double strongOrWeakModifier(RoundAction attackerAction, Gotchi gotchi, AttackType defenderGotchiType){
        AttackType attackerAttackType = null;
        if (attackerAction == RoundAction.PRIMARY_ATTACK){
            attackerAttackType = gotchi.getType();
        }
        else if (attackerAction == RoundAction.SECONDARY_ATTACK){
            attackerAttackType = gotchi.getSecondaryAttack();
        }
        else {
            return 0;
        }
        ArrayList strong = strongAgainst.get(attackerAttackType);
        ArrayList weak = weakAgainst.get(attackerAttackType);
        if (strong.contains(defenderGotchiType)){
            System.out.println(attackerAttackType + " >>>>> " + defenderGotchiType);
            System.out.println("SUPER EFFECTIVE");
            return 2;
        }
        if (weak.contains(defenderGotchiType)){
            System.out.println(attackerAttackType + " >>>>> " + defenderGotchiType);
            System.out.println("NOT VERY EFFECTIVE");
            return 0.75;
        }
        System.out.println("NORMAL HIT");
        return 1;
    }

    private RoundAction chooseRandomAction(){
        UtilRandom r = new UtilRandom();
        double randomize = r.doubleFromRange(0, 1);
        if (randomize <= 0.5){
            return RoundAction.PRIMARY_ATTACK;
        }
        else {
            return RoundAction.SECONDARY_ATTACK;
        }
    }

    private boolean succesOfEvade(int evaderSpeed, int attackerSpeed){
        double DOWN_LIMIT = 0.75;
        double UPPER_LIMIT = 1.25;
        UtilRandom r = new UtilRandom();
        if (evaderSpeed*r.doubleFromRange(DOWN_LIMIT, UPPER_LIMIT)
                > attackerSpeed*r.doubleFromRange(DOWN_LIMIT, UPPER_LIMIT)){
            return true;
        }
        else return false;
    }

    private int calculateHpLoss(double hit, double defence){
        int UPPER_RANGE = 20;
        int LOWER_RANGE = 20;
        double modifier;
        if (hit > defence + UPPER_RANGE){
            modifier = 0.5;
        }
        else if (hit >= defence && hit <= defence + UPPER_RANGE){
            return 10;
        }
        else if (hit >= defence - LOWER_RANGE){
            return 5;
        }
        else modifier = 0;
        Double hpLossDouble = new  Double(modifier * (hit - defence));

        return hpLossDouble.intValue();
    }

    private double calculateHit(){

        return 0;
    }

    @Override
    public RoundMessage sendResults(Room room) {
        System.out.println("Preparing message...");
        System.out.println(roomRoundMessageMap.get(room).getOwnerAction());
        System.out.println(roomRoundMessageMap.get(room).getOwnerActionType());
        System.out.println(roomRoundMessageMap.get(room).getOpponentAction());
        System.out.println(roomRoundMessageMap.get(room).getOpponentActionType());
        return roomRoundMessageMap.get(room);
    }

    @Override
    public void startGame(Room room) {
        System.out.println("START");
        roomRoundMessageMap.put(room, new RoundMessage());
    }

    @Override
    public HashMap<Room, RoundMessage> getMap(){
        return roomRoundMessageMap;
    }
}
