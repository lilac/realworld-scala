package articles.services

import commons.models.Email
import articles.models.{ Article, ArticleWithTags, FavoriteAssociation, FavoriteAssociationId }
import articles.repositories.{ ArticleRepo, ArticleWithTagsRepo, FavoriteAssociationRepo }
import users.models.User
import users.repositories.UserRepo
import slick.dbio.DBIO
import scala.concurrent.ExecutionContext

import commons.exceptions.MissingModelException
import commons.utils.DbioUtils

trait ArticleFavoriteService {
  protected val articleRepo: ArticleRepo
  protected val articleWithTagsRepo: ArticleWithTagsRepo
  protected val userRepo: UserRepo
  protected val favoriteAssociationRepo: FavoriteAssociationRepo

  implicit protected val ex: ExecutionContext

  def favorite(slug: String, currentUserEmail: Email): DBIO[ArticleWithTags] = {
    require(slug != null && currentUserEmail != null)

    for {
      user <- userRepo.findByEmail(currentUserEmail)
      article <- articleRepo.findBySlug(slug)
      _ <- createFavoriteAssociation(user, article)
      articleWithTags <- articleWithTagsRepo.getArticleWithTags(article, currentUserEmail)
    } yield articleWithTags
  }

  private def createFavoriteAssociation(user: User, article: Article) = {
    val favoriteAssociation = FavoriteAssociation(FavoriteAssociationId(-1), user.id, article.id)
    favoriteAssociationRepo.insert(favoriteAssociation)
  }

  def unfavorite(slug: String, currentUserEmail: Email): DBIO[ArticleWithTags] = {
    require(slug != null && currentUserEmail != null)

    for {
      user <- userRepo.findByEmail(currentUserEmail)
      article <- articleRepo.findBySlug(slug)
      _ <- deleteFavoriteAssociation(user, article)
      articleWithTags <- articleWithTagsRepo.getArticleWithTags(article, currentUserEmail)
    } yield articleWithTags
  }

  private def deleteFavoriteAssociation(user: User, article: Article) = {
    for {
      option <- favoriteAssociationRepo.findByUserAndArticle(user.id, article.id)
      association <- DbioUtils.optionToDbio(option,
        new MissingModelException(s"User ${user.username} favorite ${article.id}"))
      id <- favoriteAssociationRepo.delete(association.id)
    } yield id
  }

}
