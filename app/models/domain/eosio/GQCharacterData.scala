// package models.domain.eosio

// import play.api.libs.json._

// object GQCharacterDataTrait extends utils.CommonImplicits
// object GQCharacterData
// object GQCharacterDataHistory {
//   val tupled = (apply: (String, String, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Long, Long) => GQCharacterDataHistory).tupled
//   def apply(v: GQCharacterData): GQCharacterDataHistory =
//       new GQCharacterDataHistory(
//           v.id,
//           v.owner,
//           v.life,
//           v.hp,
//           v.ghost_class,
//           v.level,
//           v.status,
//           v.attack,
//           v.defense,
//           v.speed,
//           v.luck,
//           v.limit,
//           v.count,
//           v.created_at)

//   // def toCharacterData(v: GQCharacterDataHistory): GQCharacterData =
//   //   new GQCharacterData(
//   //       v.id,
//   //       v.owner,
//   //       v.character_life,
//   //       v.initial_hp,
//   //       v.ghost_class,
//   //       v.ghost_level,
//   //       v.status,
//   //       v.attack,
//   //       v.defense,
//   //       v.speed,
//   //       v.luck,
//   //       v.prize,
//   //       v.battle_limit,
//   //       v.battle_count,
//   //       v.last_match,
//   //       v.created_at)
// }
// trait GQCharacterDataTrait {
//     val id: String
//     val owner: String
//     val ghost_class: Int
//     val level: Int
//     def toJson(): JsValue = Json.toJson(this)
// }
// case class GQCharacterData(
//     id: String,
//     owner: String,
//     life: Int,
//     hp: Int,
//     ghost_class: Int,
//     level: Int,
//     status: Int,
//     attack: Int,
//     defense: Int,
//     speed: Int,
//     luck: Int,
//     limit: Int,
//     count: Int,
//     created_at: Long) extends GQCharacterDataTrait
// case class GQCharacterDataHistory(
//     id: String,
//     owner: String,
//     life: Int,
//     hp: Int,
//     ghost_class: Int,
//     level: Int,
//     status: Int,
//     attack: Int,
//     defense: Int,
//     speed: Int,
//     luck: Int,
//     limit: Int,
//     count: Int,
//     created_at: Long) extends GQCharacterDataTrait

