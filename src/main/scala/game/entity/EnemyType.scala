package game.entity

import data.Enemies.EnemyReference

case class EnemyTypeComponent(enemyType: EnemyReference) extends Component

object EnemyTypeComponent {
  extension (entity: Entity) {
    def enemyType: Option[EnemyReference] = entity.get[EnemyTypeComponent].map(_.enemyType)
    
    def enemyTypeName: String = entity.get[EnemyTypeComponent] match {
      case Some(component) => component.enemyType match {
        case EnemyReference.Bat => "Bat"
        case EnemyReference.Rat => "Rat"
        case EnemyReference.Snake => "Snake"
        case EnemyReference.Slime => "Slime"
        case EnemyReference.Slimelet => "Slimelet"
        case EnemyReference.Boss => "Boss"
      }
      case None => "Enemy"
    }
  }
}