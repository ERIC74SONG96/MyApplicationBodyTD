package com.example.myapplicationbodytd.towers

import android.graphics.Color
import android.graphics.PointF
import com.example.myapplicationbodytd.enemies.Enemy

class BasicTower(position: PointF) : Tower(position) {
    override val type = TowerType.BASIC
    override val range = 200f
    override val damage = 10f
    override val attackSpeed = 1.5f
    override val upgradeCost = 100
    override val maxLevel = 3

    override fun attack(target: Enemy) {
        target.position.let {
            projectiles.add(Projectile(
                startPosition = PointF(position.x, position.y),
                damage = damage,
                color = Color.GRAY
            ))
        }
    }

    override fun createProjectile(targetPosition: PointF): Projectile {
        return Projectile(
            startPosition = PointF(position.x, position.y),
            damage = damage,
            color = Color.GRAY
        )
    }
}

