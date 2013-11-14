/*
T=>M[T] : constructor

T=>M[t] : Unit

M[t]=>(T=>M[U])=>M[U] : Bind : M[t]의 원소에 새로운 정보 추가

--
*/
//Boxed:

case class Boxed[T](value:T)

def doubleBoxed(x:Int):Boxed[Int] = Boxed(x+x)
def sqrtBoxed(x:Int):Boxed[Int] = Boxed(Math.sqrt(x).toInt)
def initBoxed(x:Int):Boxed[Int] = Boxed(x)

//Logged :

case class Logged[T](value:T, log:List[String])

def doubleLogged(x:Int):Logged[Int] = Logged(x+x, List("double("+x+") = " + (x+x)))
def sqrtLogged(x:Int):Logged[Int] = Logged(Math.sqrt(x).toInt, List("sqrt("+x+").toInt = " + Math.sqrt(x).toInt))
def initLogged(x:Int):Logged[Int] = Logged(x, List())

// 값이 정상이면 MySome(값), 값이 비정상이면  MyNone인 클래스
abstract class MyOption[+A]
case class MySome[+A](x: A) extends MyOption[A]
case object MyNone extends MyOption[Nothing] 

def doubleMyOption(x:Int):MyOption[Int] = MySome(x+x)
def sqrtMyOption(x:Int):MyOption[Int] = if(x>=0) MySome(Math.sqrt(x).toInt) else MyNone
def initMyOption(x:Int):MyOption[Int] = MySome(x)


// 지연된 계산을 저장함
class Lazy[T](value: ()=>T) {
   def getValue():T = value()
}

object Lazy {
   def apply[T](v: ()=>T):Lazy[T] = 
   {
     new Lazy(v)
   }
}

def doubleLazy(x: =>Int):Lazy[Int] = Lazy(()=>{print("lazy double(" + x + ") run\n");x+x})
def sqrtLazy(x: =>Int):Lazy[Int] = Lazy(()=>{print("lazy sqrt(" + x + ") run\n");Math.sqrt(x).toInt})
def initLazy(x: =>Int):Lazy[Int] = Lazy(()=>x)

// 리스트
abstract class MyList[+A] 
case class Cons[B](var hd: B, var tl: MyList[B]) extends MyList[B]
case object MyNil extends MyList[Nothing]

// 리스트 만들기 예제
val x1:MyList[Int] = Cons(1,Cons(2,Cons(8,MyNil)))

def doubleMyList(x:Int):MyList[Int] = Cons(x+x,MyNil)
def sqrtMyList(x:Int):MyList[Int] = Cons(Math.sqrt(x).toInt,MyNil)
def initMyList(x:Int):MyList[Int] = Cons(x,MyNil)

/*
하고 싶은 일: 두 함수 f: T=>M[T]와 g: U=>M[V]를 함성하기(왜? 프로그램은 함수의 합성이 계속 적용되어 이루어짐)
이때 M의 특성에 따라 합성 결과 M[V]가 적절한 내용을 포함하고 있어야 함(왜? 기껏 작업한 중간 값이 날라가면 안 좋잖아..)

함수의 합성:
*/

def o[T,V,U](f:T=>V, g:V=>U) = (x:T) => g(f(x))

/*
이렇게 할 수 있는 이유: 

- 타입이 들어맞음

위 T=>M[V]와 V=>M[U]의 합성 :

- 일단 타입이 안 맞음.
- 따라서 V=>M(U)에서 M[V]=>M[U]를 만들어 낼 수 있어야 함.
  이를 mkXXXFun이라 하자.

일단 예제로 T=V=U=Int인 경우(우리가 앞애서 한 doubleXXX, sqrtXXX등의 예)를 보자.

*/

def mkBoxedFun(f:Int=>Boxed[Int]) = (x:Boxed[Int]) => {
	val value = x.value    // x(박싱된 값)에서 내부의 값 노출시키기
	val value2 = f(value)  // 함수 적용
	value2				   // 값 반환 
}

// doubleBoxed와 sqrtBoxed 합성해 사용해 보기
// sqrt(8+8) = Boxed(4)가 결과값임

val x2 = o(mkBoxedFun(doubleBoxed), mkBoxedFun(sqrtBoxed))(initBoxed(8))

//scala> val x2 = o(mkBoxedFun(doubleBoxed), mkBoxedFun(sqrtBoxed))(initBoxed(8))
//x: Boxed[Int] = Boxed(4)

/*
문제 없어 보인다... 

그럼 Logged에 대해 해보자. 위 mkBoxedFun을 참조해 다음과 같이 할 수 있을 것 같다.
*/
def mkLoggedFun(f:Int=>Logged[Int]) = (x:Logged[Int]) => {
	val value = x.value    // x(로그 포함된 값)에서 내부의 값 노출시키기
	val value2 = f(value)  // 함수 적용
	value2				   // 값 반환 
}

/*
합성해보자.
*/
val x3 = o(mkLoggedFun(doubleLogged), mkLoggedFun(sqrtLogged))(initLogged(8))
/*
scala> val x3 = o(mkLoggedFun(doubleLogged), mkLoggedFun(sqrtLogged))(initLogged(8))
x1: Logged[Int] = Logged(4,List(sqrt(16).toInt = 4))

문제 없나? 아니다. List에 보면 앞쪽 로그는 다 사라졌다. 함수 적용후 나온 value2에는 다른 정보는 없고, f를 적용한 정보만 
있기 때문이다. 역시 mkXXXFun도 구체적인 XXX 클래스의 종류에 따라 주의깊게 설계해야 함을 알 수 있다.

다시 로그를 합치도록 해보자.
*/
def mkLoggedFunRevised(f:Int=>Logged[Int]) = (x:Logged[Int]) => {
	val value = x.value    // x(로그 포함된 값)에서 내부의 값 노출시키기
	val log = x.log        // x에서 로그 가져오기
	val value2 = f(value)  // 함수 적용
	Logged(value2.value, log:::value2.log)
}

/*
이제 실험해보자.
*/
val x4 = o(mkLoggedFunRevised(doubleLogged), mkLoggedFunRevised(sqrtLogged))(initLogged(8))
/*
scala> val x4 = o(mkLoggedFunRevised(doubleLogged), mkLoggedFunRevised(sqrtLogged))(initLogged(8))
x4: Logged[Int] = Logged(4,List(sqrt(16).toInt = 4))

좋다. 나머지 클래스에 대해서도 구현 가능하다.
*/

// Lazy
def mkLazyFun(f: (Int=>Lazy[Int])) = (x: Lazy[Int]) => {
	def value = x.getValue     // x서 내부의 값 노출시키기(def로 했으므로 계산하지 않음)
	def tmpFun() = {	// x 내부 값을 계산하지 않게 함수를 하나 정의
		val y = f(value)
		y.getValue()
	}
	Lazy(tmpFun)
}

val x5 = o(mkLazyFun((x)=>doubleLazy(x)), mkLazyFun((y)=>sqrtLazy(y)))(initLazy(8))
x5.getValue()

/*
scala> val x5 = o(mkLazyFun((x)=>doubleLazy(x)), mkLazyFun((y)=>sqrtLazy(y)))(initLazy(8))
x5: Lazy[Int] = Lazy@7bdd5b15

scala> x5.getValue()
lazy double(8) run
lazy sqrt(16) run
res44: Int = 4
*/

// 리스트
def mkMyListFun(f:Int=>MyList[Int]) = (x:MyList[Int]) => {
  // f가 만드는 리스트를 모두 합쳐야 하기 때문에 결과적으로는 
  // flatMap과 비슷한 일을 해야 한다.
  def append(l1:MyList[Int], l2:MyList[Int]):MyList[Int] = l1 match {
    case Cons(h,t) => {
      Cons(h,append(t,l2))
    }
    case MyNil => l2
  }
  def mapAll(l:MyList[Int]):MyList[Int] = l match {
    case Cons(h,t) => {
      val value2 = f(h)
      val remain = mapAll(t)
      append(value2,remain)
    }
    case MyNil => MyNil
  }
  mapAll(x)
}

// x1은 앞에서 만들었던 리스트이다.
// x1 = Cons(1,Cons(2,Cons(8,MyNil)))
val x6 = o(mkMyListFun(doubleMyList), mkMyListFun(sqrtMyList))(x1)


