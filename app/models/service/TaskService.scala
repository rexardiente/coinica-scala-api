package models.service

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import models.domain.{ PaginatedResult, Task }
import models.repo.TaskRepo

@Singleton 
class TaskService @Inject()(taskRepo: TaskRepo ) {
  def paginatedResult[T >: Task](limit: Int, offset: Int): Future[PaginatedResult[T]] = 
    for {
      task <- taskRepo.findAll(limit, offset)
      size <- taskRepo.getSize()
      hasNext <- Future(size - (offset + limit) > 0)
    } yield PaginatedResult(size, task.toList, hasNext)
}