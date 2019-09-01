package articles

import scala.concurrent.ExecutionContext

import articles.controllers.{ ArticleController, CommentController, TagController }
import articles.models.{ ArticleMetaModel, CommentId, MainFeedPageRequest, UserFeedPageRequest }
import articles.repositories._
import articles.services._
import com.softwaremill.macwire.{ Module, wire }
import commons.models._
import play.api.ApplicationLoader
import play.api.routing.Router
import play.api.routing.sird._
import users.UserComponents

@Module
class ArticleComponents(context: ApplicationLoader.Context)
  extends UserComponents(context) {

  private lazy val defaultOffset = 0L
  private lazy val defaultLimit = 20L
  private implicit val ec: ExecutionContext = executionContext

  lazy val articleController: ArticleController = wire[ArticleController]
  lazy val articleWriteService: ArticleWriteService = wire[ArticleWriteService]
  lazy val articleReadService: ArticleReadService = wire[ArticleReadService]
  lazy val articleRepo: ArticleRepo = wire[ArticleRepo]

  lazy val commentController: CommentController = wire[CommentController]
  lazy val commentService: CommentService = wire[CommentService]
  lazy val commentWithAuthorRepo: CommentWithAuthorRepo = wire[CommentWithAuthorRepo]
  lazy val commentRepo: CommentRepo = wire[CommentRepo]

  lazy val tagController: TagController = wire[TagController]
  lazy val tagService: TagService = wire[TagService]
  lazy val tagRepo: TagRepo = wire[TagRepo]

  lazy val articleTagRepo: ArticleTagAssociationRepo = wire[ArticleTagAssociationRepo]
  lazy val articleWithTagsRepo: ArticleWithTagsRepo = wire[ArticleWithTagsRepo]

  lazy val favoriteAssociationRepo: FavoriteAssociationRepo = wire[FavoriteAssociationRepo]

  override val routes: Router.Routes = {
    case GET(p"/articles" ? q_o"limit=${long(maybeLimit)}" &
      q_o"offset=${long(maybeOffset)}" &
      q_o"tag=$maybeTag" &
      q_o"author=$maybeAuthor" &
      q_o"favorited=$maybeFavorited") =>

      val limit = maybeLimit.getOrElse(defaultLimit)
      val offset = maybeOffset.getOrElse(defaultOffset)
      val maybeAuthorUsername = maybeAuthor.map(Username(_))
      val maybeFavoritedUsername = maybeFavorited.map(Username(_))

      articleController.findAll(MainFeedPageRequest(maybeTag, maybeAuthorUsername, maybeFavoritedUsername, limit, offset,
        List(Ordering(ArticleMetaModel.createdAt, Descending))))
    case GET(p"/articles/feed" ? q_o"limit=${long(limit)}" & q_o"offset=${long(offset)}") =>
      val theLimit = limit.getOrElse(defaultLimit)
      val theOffset = offset.getOrElse(defaultOffset)

      articleController.findFeed(UserFeedPageRequest(theLimit, theOffset,
        List(Ordering(ArticleMetaModel.createdAt, Descending))))
    case GET(p"/articles/$slug") =>
      articleController.findBySlug(slug)
    case POST(p"/articles") =>
      articleController.create
    case PUT(p"/articles/$slug") =>
      articleController.update(slug)
    case DELETE(p"/articles/$slug") =>
      articleController.delete(slug)
    case POST(p"/articles/$slug/comments") =>
      commentController.create(slug)
    case GET(p"/articles/$slug/comments") =>
      commentController.findByArticleSlug(slug)
    case POST(p"/articles/$slug/favorite") =>
      articleController.favorite(slug)
    case DELETE(p"/articles/$slug/favorite") =>
      articleController.unfavorite(slug)
    case DELETE(p"/articles/$_/comments/${long(id)}") =>
      commentController.delete(CommentId(id))
    case GET(p"/tags") =>
      tagController.findAll
  }

}