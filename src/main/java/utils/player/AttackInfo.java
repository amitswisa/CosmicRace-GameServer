package utils.player;

import dto.PlayerCommand;
import entities.player.WebPlayerEntity;
import model.player.PlayerEntity;
import utils.loggers.LoggerManager;

import java.util.List;
import java.util.Random;

import static utils.GlobalSettings.ATTACK_COOLDOWN;

public class AttackInfo {

    private String m_AttackerName;
    private String m_Victim;
    private String m_AttackID;

    public AttackInfo(String m_AttackerName, String m_Victim, String m_AttackID) {
        this.m_AttackerName = m_AttackerName;
        this.m_Victim = m_Victim;
        this.m_AttackID = m_AttackID;
    }

    public AttackInfo() {}

    public void SetAttackerName(String m_AttackerName) {
        this.m_AttackerName = m_AttackerName;
    }

    public void SetVictimName(String m_Victim) {
        this.m_Victim = m_Victim;
    }

    public void SetAttackID(String m_AttackID) {
        this.m_AttackID = m_AttackID;
    }

    public static PlayerEntity GetPlayer(String i_Name, List<PlayerEntity> m_MatchPlayerEntities){
        for (PlayerEntity player : m_MatchPlayerEntities)
        {
            if(player.GetUserName() != null
                    && player.GetUserName().equals(i_Name))
            {
                return player;
            }
        }
        return null;
    }
    /**
     * Return attack information.
     * In case the attacker can't attack, since he has cool-down - returns NULL*/
    public static AttackInfo GenerateAttackInfo(PlayerCommand i_PlayerCommand, List<PlayerEntity> i_MatchPlayerEntities) {

        int attackerInd = 0;
        AttackInfo attackInfo = new AttackInfo();
        PlayerEntity attacker = GetPlayer(i_PlayerCommand.GetUsername(), i_MatchPlayerEntities);

        if(System.currentTimeMillis() - attacker.GetLastAttackTime() >= ATTACK_COOLDOWN) { // if the cool-down is over. He is able to attack
            attackInfo.SetAttackerName(attacker.GetUserName());
//        attackInfo.SetAttackID(i_PlayerCommand.);

            for (int i = 0; i < i_MatchPlayerEntities.size(); i++) {
                if (attacker.GetUserName().equals(i_MatchPlayerEntities.get(i).GetUserName())) {
                    attackerInd = i;
                    break;
                }
            }

            Random random = new Random();

            while (!attacker.IsDead()) {
                int victimInd = random.nextInt(i_MatchPlayerEntities.size());

                if (victimInd != attackerInd) {

                    PlayerEntity victim = i_MatchPlayerEntities.get(victimInd);
                    if(!victim.IsDead()){

                        attackInfo.SetVictimName(victim.GetUserName());
                        attacker.SetLastAttackTime();
                        victim.MarkDead();
                        LoggerManager.info("Attack occur: " + attacker.GetUserName() + " attacked " + victim.GetUserName());
                        break;
                    }
                }
            }
        }

        return attackInfo;
    }
}
